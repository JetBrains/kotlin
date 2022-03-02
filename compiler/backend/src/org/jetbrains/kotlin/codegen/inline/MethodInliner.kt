/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.coroutines.CONTINUATION_ASM_TYPE
import org.jetbrains.kotlin.codegen.inline.FieldRemapper.Companion.foldName
import org.jetbrains.kotlin.codegen.inline.coroutines.CoroutineTransformer
import org.jetbrains.kotlin.codegen.inline.coroutines.markNoinlineLambdaIfSuspend
import org.jetbrains.kotlin.codegen.inline.coroutines.surroundInvokesWithSuspendMarkersIfNeeded
import org.jetbrains.kotlin.codegen.optimization.ApiVersionCallsPreprocessingMethodTransformer
import org.jetbrains.kotlin.codegen.optimization.FixStackWithLabelNormalizationMethodTransformer
import org.jetbrains.kotlin.codegen.optimization.common.*
import org.jetbrains.kotlin.codegen.optimization.fixStack.*
import org.jetbrains.kotlin.codegen.optimization.nullCheck.isCheckParameterIsNotNull
import org.jetbrains.kotlin.codegen.optimization.temporaryVals.TemporaryVariablesEliminationTransformer
import org.jetbrains.kotlin.codegen.pseudoInsns.PseudoInsn
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.SmartSet
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.commons.LocalVariablesSorter
import org.jetbrains.org.objectweb.asm.commons.MethodRemapper
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame
import org.jetbrains.org.objectweb.asm.util.Printer
import java.util.*
import kotlin.math.max

class MethodInliner(
    private val node: MethodNode,
    private val parameters: Parameters,
    private val inliningContext: InliningContext,
    private val nodeRemapper: FieldRemapper,
    private val isSameModule: Boolean,
    private val errorPrefix: String,
    private val sourceMapper: SourceMapCopier,
    private val inlineCallSiteInfo: InlineCallSiteInfo,
    private val inlineOnlySmapSkipper: InlineOnlySmapSkipper?, //non null only for root
    private val shouldPreprocessApiVersionCalls: Boolean = false
) {
    private val languageVersionSettings = inliningContext.state.languageVersionSettings
    private val invokeCalls = ArrayList<InvokeCall>()

    //keeps order
    private val transformations = ArrayList<TransformationInfo>()

    //current state
    private val currentTypeMapping = HashMap<String, String?>()
    private val result = InlineResult.create()
    private var lambdasFinallyBlocks: Int = 0

    fun doInline(
        adapter: MethodVisitor,
        remapper: LocalVarRemapper,
        remapReturn: Boolean,
        returnLabels: Map<String, Label?>
    ): InlineResult {
        return doInline(adapter, remapper, remapReturn, returnLabels, 0)
    }

    private fun recordTransformation(info: TransformationInfo) {
        if (!inliningContext.isInliningLambda) {
            inliningContext.root.state.globalInlineContext.recordTypeFromInlineFunction(info.oldClassName)
        }
        if (info.shouldRegenerate(isSameModule)) {
            inliningContext.recordRegeneratedAnonymousObject(info.oldClassName)
        }
        transformations.add(info)
    }

    private fun doInline(
        adapter: MethodVisitor,
        remapper: LocalVarRemapper,
        remapReturn: Boolean,
        returnLabels: Map<String, Label?>,
        finallyDeepShift: Int
    ): InlineResult {
        //analyze body
        var transformedNode = markPlacesForInlineAndRemoveInlinable(node, returnLabels, finallyDeepShift)

        //substitute returns with "goto end" instruction to keep non local returns in lambdas
        val end = linkedLabel()
        val isTransformingAnonymousObject = nodeRemapper is RegeneratedLambdaFieldRemapper
        transformedNode = doInline(transformedNode)
        if (!isTransformingAnonymousObject) {
            //don't remove assertion in transformed anonymous object
            removeClosureAssertions(transformedNode)
        }
        transformedNode.instructions.resetLabels()

        val resultNode = MethodNode(
            Opcodes.API_VERSION, transformedNode.access, transformedNode.name, transformedNode.desc,
            transformedNode.signature, transformedNode.exceptions?.toTypedArray()
        )

        val visitor = RemapVisitor(resultNode, remapper, nodeRemapper)

        try {
            transformedNode.accept(
                if (isTransformingAnonymousObject) {
                    /*keep annotations and attributes during anonymous object transformations*/
                    visitor
                } else MethodBodyVisitor(visitor)
            )
        } catch (e: Throwable) {
            throw wrapException(e, transformedNode, "couldn't inline method call")
        }

        resultNode.visitLabel(end)

        if (inliningContext.isRoot) {
            val remapValue = remapper.remap(parameters.argsSizeOnStack + 1).value
            InternalFinallyBlockInliner.processInlineFunFinallyBlocks(
                resultNode, lambdasFinallyBlocks, (remapValue as StackValue.Local).index,
                languageVersionSettings.supportsFeature(LanguageFeature.ProperFinally)
            )
        }

        if (remapReturn) {
            processReturns(resultNode, returnLabels, end)
        }
        //flush transformed node to output
        resultNode.accept(SkipMaxAndEndVisitor(adapter))
        return result
    }

    private fun doInline(node: MethodNode): MethodNode {
        val currentInvokes = LinkedList(invokeCalls)

        val resultNode = MethodNode(node.access, node.name, node.desc, node.signature, null)

        val iterator = transformations.iterator()

        val remapper = TypeRemapper.createFrom(currentTypeMapping)

        // MethodRemapper doesn't extends LocalVariablesSorter, but RemappingMethodAdapter does.
        // So wrapping with LocalVariablesSorter to keep old behavior
        // TODO: investigate LocalVariablesSorter removing (see also same code in RemappingClassBuilder.java)
        val localVariablesSorter = LocalVariablesSorter(resultNode.access, resultNode.desc, wrapWithMaxLocalCalc(resultNode))
        val remappingMethodAdapter = MethodRemapper(localVariablesSorter, AsmTypeRemapper(remapper, result))

        val fakeContinuationName = CoroutineTransformer.findFakeContinuationConstructorClassName(node)
        val markerShift = calcMarkerShift(parameters, node)
        val lambdaInliner = object : InlineAdapter(remappingMethodAdapter, parameters.argsSizeOnStack, sourceMapper) {
            private var transformationInfo: TransformationInfo? = null

            private fun handleAnonymousObjectRegeneration() {
                transformationInfo = iterator.next()

                val oldClassName = transformationInfo!!.oldClassName
                if (inliningContext.parent?.transformationInfo?.oldClassName == oldClassName) {
                    // Object constructs itself, don't enter an infinite recursion of regeneration.
                } else if (transformationInfo!!.shouldRegenerate(isSameModule)) {
                    //TODO: need poping of type but what to do with local funs???
                    val newClassName = transformationInfo!!.newClassName
                    remapper.addMapping(oldClassName, newClassName)

                    val childInliningContext = inliningContext.subInlineWithClassRegeneration(
                        inliningContext.nameGenerator,
                        currentTypeMapping,
                        inlineCallSiteInfo,
                        transformationInfo!!
                    )
                    val transformer = transformationInfo!!.createTransformer(
                        childInliningContext, isSameModule, fakeContinuationName
                    )

                    val transformResult = transformer.doTransform(nodeRemapper)
                    result.merge(transformResult)
                    result.addChangedType(oldClassName, newClassName)

                    if (inliningContext.isInliningLambda &&
                        inliningContext.lambdaInfo !is DefaultLambda && //never delete default lambda classes
                        transformationInfo!!.canRemoveAfterTransformation() &&
                        !inliningContext.root.state.globalInlineContext.isTypeFromInlineFunction(oldClassName)
                    ) {
                        // this class is transformed and original not used so we should remove original one after inlining
                        result.addClassToRemove(oldClassName)
                    }

                    if (transformResult.reifiedTypeParametersUsages.wereUsedReifiedParameters()) {
                        ReifiedTypeInliner.putNeedClassReificationMarker(mv)
                        result.reifiedTypeParametersUsages.mergeAll(transformResult.reifiedTypeParametersUsages)
                    }

                    for (classBuilder in childInliningContext.continuationBuilders.values) {
                        classBuilder.done()
                    }
                } else {
                    result.addNotChangedClass(oldClassName)
                }
            }

            override fun anew(type: Type) {
                if (isSamWrapper(type.internalName) || isAnonymousClass(type.internalName)) {
                    handleAnonymousObjectRegeneration()
                }

                //in case of regenerated transformationInfo type would be remapped to new one via remappingMethodAdapter
                super.anew(type)
            }

            override fun visitMethodInsn(opcode: Int, owner: String, name: String, desc: String, itf: Boolean) {
                if (/*INLINE_RUNTIME.equals(owner) &&*/ isInvokeOnLambda(owner, name)) { //TODO add method
                    assert(!currentInvokes.isEmpty())
                    val invokeCall = currentInvokes.remove()
                    val info = invokeCall.functionalArgument

                    if (info !is LambdaInfo) {
                        //noninlinable lambda
                        markNoinlineLambdaIfSuspend(mv, info)
                        super.visitMethodInsn(opcode, owner, name, desc, itf)
                        return
                    }

                    val nullableAnyType = inliningContext.state.module.builtIns.nullableAnyType
                    val expectedParameters = info.invokeMethod.argumentTypes
                    val expectedKotlinParameters = info.invokeMethodParameters
                    val argumentCount = Type.getArgumentTypes(desc).size.let {
                        if (info is PsiExpressionLambda && info.invokeMethodDescriptor.isSuspend && it < expectedParameters.size) {
                            // Inlining suspend lambda into a function that takes a non-suspend lambda.
                            // In the IR backend, this cannot happen as inline lambdas are not lowered.
                            addFakeContinuationMarker(this)
                            it + 1
                        } else it
                    }
                    assert(argumentCount == expectedParameters.size && argumentCount == expectedKotlinParameters.size) {
                        "inconsistent lambda arguments: $argumentCount on stack, ${expectedParameters.size} expected, " +
                                "${expectedKotlinParameters.size} Kotlin types"
                    }

                    var valueParamShift = max(nextLocalIndex, markerShift) + expectedParameters.sumOf { it.size }
                    for (index in argumentCount - 1 downTo 0) {
                        val type = expectedParameters[index]
                        StackValue.coerce(AsmTypes.OBJECT_TYPE, nullableAnyType, type, expectedKotlinParameters[index], this)
                        valueParamShift -= type.size
                        store(valueParamShift, type)
                    }
                    if (expectedParameters.isEmpty()) {
                        nop() // add something for a line number to bind onto
                    }

                    inlineOnlySmapSkipper?.onInlineLambdaStart(remappingMethodAdapter, info.node.node, sourceMapper.parent)
                    addInlineMarker(this, true)
                    val lambdaParameters = info.addAllParameters(nodeRemapper)

                    val newCapturedRemapper = InlinedLambdaRemapper(
                        info.lambdaClassType.internalName, nodeRemapper, lambdaParameters,
                        info is DefaultLambda && info.isBoundCallableReference
                    )

                    setLambdaInlining(true)

                    val callSite = sourceMapper.callSite.takeIf { info is DefaultLambda }
                    val inliner = MethodInliner(
                        info.node.node, lambdaParameters, inliningContext.subInlineLambda(info),
                        newCapturedRemapper,
                        if (info is DefaultLambda) isSameModule else true /*cause all nested objects in same module as lambda*/,
                        "Lambda inlining " + info.lambdaClassType.internalName,
                        SourceMapCopier(sourceMapper.parent, info.node.classSMAP, callSite), inlineCallSiteInfo, null
                    )

                    val varRemapper = LocalVarRemapper(lambdaParameters, valueParamShift)
                    //TODO add skipped this and receiver
                    val lambdaResult =
                        inliner.doInline(localVariablesSorter, varRemapper, true, info.returnLabels, invokeCall.finallyDepthShift)
                    result.mergeWithNotChangeInfo(lambdaResult)
                    result.reifiedTypeParametersUsages.mergeAll(lambdaResult.reifiedTypeParametersUsages)
                    result.reifiedTypeParametersUsages.mergeAll(info.reifiedTypeParametersUsages)

                    StackValue.coerce(info.invokeMethod.returnType, info.invokeMethodReturnType, OBJECT_TYPE, nullableAnyType, this)
                    setLambdaInlining(false)
                    addInlineMarker(this, false)
                    inlineOnlySmapSkipper?.onInlineLambdaEnd(remappingMethodAdapter)
                } else if (isAnonymousConstructorCall(owner, name)) { //TODO add method
                    //TODO add proper message
                    val newInfo = transformationInfo as? AnonymousObjectTransformationInfo ?: throw AssertionError(
                        "<init> call doesn't correspond to object transformation info for '$owner.$name': $transformationInfo"
                    )
                    // inline fun f() -> new f$1 -> fun something() in class f$1 -> new f$1
                    //                   ^-- fetch the info that was created for this instruction
                    // Currently, this self-reference pattern only happens in coroutine objects, which construct
                    // copies of themselves in `create` or `invoke` (not `invokeSuspend`).
                    val existingInfo = inliningContext.parent?.transformationInfo?.takeIf { it.oldClassName == newInfo.oldClassName }
                            as AnonymousObjectTransformationInfo?
                    val info = existingInfo ?: newInfo
                    if (info.shouldRegenerate(isSameModule)) {
                        for (capturedParamDesc in info.allRecapturedParameters) {
                            val realDesc = if (existingInfo != null && capturedParamDesc.fieldName == AsmUtil.THIS) {
                                // The captures in `info` are relative to the parent context, so a normal `this` there
                                // is a captured outer `this` here.
                                CapturedParamDesc(Type.getObjectType(owner), AsmUtil.CAPTURED_THIS_FIELD, capturedParamDesc.type)
                            } else capturedParamDesc
                            visitFieldInsn(
                                Opcodes.GETSTATIC, realDesc.containingLambdaName,
                                foldName(realDesc.fieldName), realDesc.type.descriptor
                            )
                        }
                        super.visitMethodInsn(opcode, info.newClassName, name, info.newConstructorDescriptor, itf)

                        //TODO: add new inner class also for other contexts
                        if (inliningContext.parent is RegeneratedClassContext) {
                            inliningContext.parent.typeRemapper.addAdditionalMappings(info.oldClassName, info.newClassName)
                        }

                        transformationInfo = null
                    } else {
                        super.visitMethodInsn(opcode, owner, name, desc, itf)
                    }
                } else if (ReifiedTypeInliner.isNeedClassReificationMarker(MethodInsnNode(opcode, owner, name, desc, false))) {
                    // If objects are reified, the marker will be recreated by `handleAnonymousObjectRegeneration` above.
                    if (!inliningContext.shouldReifyTypeParametersInObjects) {
                        super.visitMethodInsn(opcode, owner, name, desc, itf)
                    }
                } else {
                    super.visitMethodInsn(opcode, owner, name, desc, itf)
                }
            }

            override fun visitFieldInsn(opcode: Int, owner: String, name: String, desc: String) {
                if (opcode == Opcodes.GETSTATIC && (isAnonymousSingletonLoad(owner, name) || isWhenMappingAccess(owner, name))) {
                    handleAnonymousObjectRegeneration()
                }
                super.visitFieldInsn(opcode, owner, name, desc)
            }

            override fun visitMaxs(stack: Int, locals: Int) {
                lambdasFinallyBlocks = resultNode.tryCatchBlocks.size
                super.visitMaxs(stack, locals)
            }
        }

        node.accept(lambdaInliner)

        surroundInvokesWithSuspendMarkersIfNeeded(resultNode)
        return resultNode
    }

    private fun prepareNode(node: MethodNode, finallyDeepShift: Int): MethodNode {
        node.instructions.resetLabels()

        val capturedParamsSize = parameters.capturedParametersSizeOnStack
        val realParametersSize = parameters.realParametersSizeOnStack
        val reorderIrLambdaParameters = inliningContext.isInliningLambda &&
                inliningContext.parent?.isInliningLambda == false &&
                inliningContext.lambdaInfo is IrExpressionLambda
        val newArgumentList = if (reorderIrLambdaParameters) {
            // In IR lambdas, captured variables come before real parameters, but after the extension receiver.
            // Move them to the end of the descriptor instead.
            Type.getArgumentTypes(inliningContext.lambdaInfo!!.invokeMethod.descriptor) + parameters.capturedTypes
        } else {
            Type.getArgumentTypes(node.desc) + parameters.capturedTypes
        }
        val transformedNode = MethodNode(
            Opcodes.API_VERSION, node.access, node.name,
            Type.getMethodDescriptor(Type.getReturnType(node.desc), *newArgumentList),
            node.signature, node.exceptions?.toTypedArray()
        )

        val transformationVisitor = object : InlineMethodInstructionAdapter(transformedNode) {
            private val GENERATE_DEBUG_INFO = GENERATE_SMAP && inlineOnlySmapSkipper == null

            private val isInliningLambda = nodeRemapper.isInsideInliningLambda

            private fun getNewIndex(`var`: Int): Int {
                val lambdaInfo = inliningContext.lambdaInfo
                if (reorderIrLambdaParameters && lambdaInfo is IrExpressionLambda) {
                    val extensionSize = if (lambdaInfo.isExtensionLambda) lambdaInfo.invokeMethod.argumentTypes[0].size else 0
                    return when {
                        //                v-- extensionSize     v-- argsSizeOnStack
                        // |- extension -|- captured -|- real -|- locals -|    old descriptor
                        // |- extension -|- real -|- captured -|- locals -|    new descriptor
                        //                         ^-- realParametersSize
                        `var` >= parameters.argsSizeOnStack -> `var`
                        `var` >= extensionSize + capturedParamsSize -> `var` - capturedParamsSize
                        `var` >= extensionSize -> `var` + realParametersSize - extensionSize
                        else -> `var`
                    }
                }
                // |- extension -|- real -|- locals -|               old descriptor
                // |- extension -|- real -|- captured -|- locals -|  new descriptor
                return `var` + if (`var` < realParametersSize) 0 else capturedParamsSize
            }

            override fun visitVarInsn(opcode: Int, `var`: Int) {
                super.visitVarInsn(opcode, getNewIndex(`var`))
            }

            override fun visitIincInsn(`var`: Int, increment: Int) {
                super.visitIincInsn(getNewIndex(`var`), increment)
            }

            override fun visitMaxs(maxStack: Int, maxLocals: Int) {
                super.visitMaxs(maxStack, maxLocals + capturedParamsSize)
            }

            override fun visitLineNumber(line: Int, start: Label) {
                if (isInliningLambda || GENERATE_DEBUG_INFO) {
                    super.visitLineNumber(line, start)
                }
            }

            override fun visitMethodInsn(opcode: Int, owner: String, name: String, desc: String, itf: Boolean) {
                if (DEFAULT_LAMBDA_FAKE_CALL == owner) {
                    val index = name.substringAfter(DEFAULT_LAMBDA_FAKE_CALL).toInt()
                    val lambda = getFunctionalArgumentIfExists(index) as DefaultLambda
                    for (captured in lambda.capturedVars.asReversed()) {
                        lambda.originalBoundReceiverType?.let {
                            // The receiver is the only captured value; it needs to be boxed.
                            StackValue.onStack(it).put(captured.type, InstructionAdapter(this))
                        }
                        super.visitFieldInsn(
                            Opcodes.PUTSTATIC,
                            captured.containingLambdaName,
                            CAPTURED_FIELD_FOLD_PREFIX + captured.fieldName,
                            captured.type.descriptor
                        )
                    }
                } else {
                    super.visitMethodInsn(opcode, owner, name, desc, itf)
                }
            }

            override fun visitLocalVariable(
                name: String, desc: String, signature: String?, start: Label, end: Label, index: Int
            ) {
                if (isInliningLambda || GENERATE_DEBUG_INFO) {
                    val isInlineFunctionMarker = name.startsWith(JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_FUNCTION)
                    val varSuffix = when {
                        inliningContext.isRoot && !isInlineFunctionMarker -> INLINE_FUN_VAR_SUFFIX
                        else -> ""
                    }

                    val varName = if (varSuffix.isNotEmpty() && name == AsmUtil.THIS) AsmUtil.INLINE_DECLARATION_SITE_THIS else name
                    super.visitLocalVariable(varName + varSuffix, desc, signature, start, end, getNewIndex(index))
                }
            }
        }

        node.accept(transformationVisitor)

        transformCaptured(transformedNode)
        transformFinallyDeepIndex(transformedNode, finallyDeepShift)

        return transformedNode
    }

    private fun markPlacesForInlineAndRemoveInlinable(
        node: MethodNode, returnLabels: Map<String, Label?>, finallyDeepShift: Int
    ): MethodNode {
        val processingNode = prepareNode(node, finallyDeepShift)

        preprocessNodeBeforeInline(processingNode, returnLabels)

        replaceContinuationAccessesWithFakeContinuationsIfNeeded(processingNode)

        val toDelete = SmartSet.create<AbstractInsnNode>()

        val sources = analyzeMethodNodeWithInterpreter(processingNode, FunctionalArgumentInterpreter(this))
        val instructions = processingNode.instructions

        markObsoleteInstruction(instructions, toDelete, sources)

        var awaitClassReification = false
        var currentFinallyDeep = 0

        InsnSequence(instructions).forEach { cur ->
            val frame = sources[instructions.indexOf(cur)]

            if (frame != null) {
                when {
                    ReifiedTypeInliner.isNeedClassReificationMarker(cur) -> awaitClassReification = true

                    cur is MethodInsnNode -> {
                        if (isFinallyStart(cur)) {
                            //TODO deep index calc could be more precise
                            currentFinallyDeep = getConstant(cur.previous)
                        }

                        val owner = cur.owner
                        val name = cur.name
                        //TODO check closure
                        val argTypes = Type.getArgumentTypes(cur.desc)
                        val paramCount = argTypes.size + 1//non static
                        val firstParameterIndex = frame.stackSize - paramCount
                        if (isInvokeOnLambda(owner, name) /*&& methodInsnNode.owner.equals(INLINE_RUNTIME)*/) {
                            val sourceValue = frame.getStack(firstParameterIndex)
                            invokeCalls.add(InvokeCall(sourceValue.functionalArgument, currentFinallyDeep))
                        } else if (isSamWrapperConstructorCall(owner, name)) {
                            recordTransformation(SamWrapperTransformationInfo(owner, inliningContext, isAlreadyRegenerated(owner)))
                        } else if (isAnonymousConstructorCall(owner, name)) {
                            val functionalArgumentMapping = HashMap<Int, FunctionalArgument>()

                            var offset = 0
                            var capturesAnonymousObjectThatMustBeRegenerated = false
                            for (i in 0 until paramCount) {
                                val sourceValue = frame.getStack(firstParameterIndex + i)
                                val functionalArgument = sourceValue.functionalArgument
                                if (functionalArgument != null) {
                                    functionalArgumentMapping[offset] = functionalArgument
                                } else if (i < argTypes.size && isAnonymousClassThatMustBeRegenerated(argTypes[i])) {
                                    capturesAnonymousObjectThatMustBeRegenerated = true
                                }

                                offset += if (i == 0) 1 else argTypes[i - 1].size
                            }

                            recordTransformation(
                                buildConstructorInvocation(
                                    owner,
                                    cur.desc,
                                    functionalArgumentMapping,
                                    awaitClassReification,
                                    capturesAnonymousObjectThatMustBeRegenerated
                                )
                            )
                            awaitClassReification = false
                        } else if (inliningContext.isInliningLambda && ReifiedTypeInliner.isOperationReifiedMarker(cur)) {
                            val reificationArgument = cur.reificationArgument
                            val parameterName = reificationArgument!!.parameterName
                            result.reifiedTypeParametersUsages.addUsedReifiedParameter(parameterName)
                        }
                    }

                    cur.opcode == Opcodes.GETSTATIC -> {
                        val fieldInsnNode = cur as FieldInsnNode?
                        val className = fieldInsnNode!!.owner
                        when {
                            isAnonymousSingletonLoad(className, fieldInsnNode.name) -> {
                                recordTransformation(
                                    AnonymousObjectTransformationInfo(
                                        className, awaitClassReification, isAlreadyRegenerated(className), true,
                                        inliningContext.nameGenerator
                                    )
                                )
                                awaitClassReification = false
                            }
                            isWhenMappingAccess(className, fieldInsnNode.name) -> {
                                recordTransformation(
                                    WhenMappingTransformationInfo(
                                        className, inliningContext.nameGenerator, isAlreadyRegenerated(className), fieldInsnNode
                                    )
                                )
                            }
                            fieldInsnNode.isCheckAssertionsStatus() -> {
                                fieldInsnNode.owner = inlineCallSiteInfo.ownerClassName
                                when {
                                    // In inline function itself:
                                    inliningContext.parent == null -> inliningContext
                                    // In method of regenerated object - field should already exist:
                                    inliningContext.parent is RegeneratedClassContext -> inliningContext.parent
                                    // In lambda inlined into the root function:
                                    inliningContext.parent.parent == null -> inliningContext.parent
                                    // In lambda inlined into a method of a regenerated object:
                                    else -> inliningContext.parent.parent as? RegeneratedClassContext
                                        ?: throw AssertionError("couldn't find class for \$assertionsDisabled (context = $inliningContext)")
                                }.generateAssertField = true
                            }
                        }
                    }

                    cur.opcode == Opcodes.POP -> {
                        if (frame.top().functionalArgument is LambdaInfo) {
                            toDelete.add(cur)
                        }
                    }

                    cur.opcode == Opcodes.PUTFIELD -> {
                        //Recognize next contract's pattern in inline lambda
                        //  ALOAD 0
                        //  SOME_VALUE
                        //  PUTFIELD $capturedField
                        // and transform it to
                        //  SOME_VALUE
                        //  PUTSTATIC $$$$capturedField
                        val fieldInsn = cur as FieldInsnNode
                        if (isCapturedFieldName(fieldInsn.name) &&
                            nodeRemapper is InlinedLambdaRemapper &&
                            nodeRemapper.originalLambdaInternalName == fieldInsn.owner
                        ) {
                            val stackTransformations = mutableSetOf<AbstractInsnNode>()
                            val lambdaInfo = frame.peek(1)?.functionalArgument
                            if (lambdaInfo is LambdaInfo && stackTransformations.all { it is VarInsnNode }) {
                                assert(lambdaInfo.lambdaClassType.internalName == nodeRemapper.originalLambdaInternalName) {
                                    "Wrong bytecode template for contract template: ${lambdaInfo.lambdaClassType.internalName} != ${nodeRemapper.originalLambdaInternalName}"
                                }
                                fieldInsn.name = foldName(fieldInsn.name)
                                fieldInsn.opcode = Opcodes.PUTSTATIC
                                toDelete.addAll(stackTransformations)
                            }
                        }
                    }
                }
            } else {
                //given frame is <tt>null</tt> if and only if the corresponding instruction cannot be reached (dead code).
                //clean dead code otherwise there is problems in unreachable finally block, don't touch label it cause try/catch/finally problems
                if (cur.type == AbstractInsnNode.LABEL) {
                    //NB: Cause we generate exception table for default handler using gaps (see ExpressionCodegen.visitTryExpression)
                    //it may occurs that interval for default handler starts before catch start label, so this label seems as dead,
                    //but as result all this labels will be merged into one (see KT-5863)
                } else {
                    toDelete.add(cur)
                }
            }
        }

        processingNode.remove(toDelete)

        //clean dead try/catch blocks
        processingNode.tryCatchBlocks.removeIf { it.isMeaningless() }

        return processingNode
    }

    private fun MethodInliner.markObsoleteInstruction(
        instructions: InsnList,
        toDelete: SmartSet<AbstractInsnNode>,
        sources: Array<out Frame<BasicValue>?>
    ) {
        for (insn in instructions) {
            // Parameter checks are processed separately
            if (insn.isAloadBeforeCheckParameterIsNotNull()) continue
            val functionalArgument = getFunctionalArgumentIfExists(insn)
            if (functionalArgument is LambdaInfo) {
                toDelete.add(insn)
            } else {
                when (insn.opcode) {
                    Opcodes.ASTORE -> {
                        if (sources[instructions.indexOf(insn)]?.top().functionalArgument is LambdaInfo) {
                            toDelete.add(insn)
                        }
                    }
                    Opcodes.SWAP -> {
                        if (sources[instructions.indexOf(insn)]?.peek(0).functionalArgument is LambdaInfo ||
                            sources[instructions.indexOf(insn)]?.peek(1).functionalArgument is LambdaInfo
                        ) {
                            toDelete.add(insn)
                        }
                    }
                    Opcodes.ALOAD -> {
                        if (sources[instructions.indexOf(insn)]?.getLocal((insn as VarInsnNode).`var`).functionalArgument is LambdaInfo) {
                            toDelete.add(insn)
                        }
                    }
                }
            }
        }
    }

    // Replace ALOAD 0
    // with
    //   ICONST fakeContinuationMarker
    //   INVOKESTATIC InlineMarker.mark
    //   ACONST_NULL
    // iff this ALOAD 0 is continuation and one of the following conditions is met
    //   1) it is passed as the last parameter to suspending function
    //   2) it is ASTORE'd right after
    //   3) it is passed to invoke of lambda
    private fun replaceContinuationAccessesWithFakeContinuationsIfNeeded(processingNode: MethodNode) {
        // in ir backend inline suspend lambdas do not use ALOAD 0 to get continuation, since they are generated as static functions
        // instead they get continuation from parameter.
        val lambdaInfo = inliningContext.lambdaInfo ?: return
        if (lambdaInfo !is PsiExpressionLambda || !lambdaInfo.invokeMethodDescriptor.isSuspend) return
        val sources = analyzeMethodNodeWithInterpreter(processingNode, Aload0Interpreter(processingNode))
        val cfg = ControlFlowGraph.build(processingNode)
        val aload0s = processingNode.instructions.asSequence().filter { it.opcode == Opcodes.ALOAD && it.safeAs<VarInsnNode>()?.`var` == 0 }

        val visited = hashSetOf<AbstractInsnNode>()
        fun findMeaningfulSuccs(insn: AbstractInsnNode): Collection<AbstractInsnNode> {
            if (!visited.add(insn)) return emptySet()
            val res = hashSetOf<AbstractInsnNode>()
            for (succIndex in cfg.getSuccessorsIndices(insn)) {
                val succ = processingNode.instructions[succIndex]
                if (succ.isMeaningful) res.add(succ)
                else res.addAll(findMeaningfulSuccs(succ))
            }
            return res
        }

        // After inlining suspendCoroutineUninterceptedOrReturn there will be suspension point, which is not a MethodInsnNode.
        // So, it is incorrect to expect MethodInsnNodes only
        val suspensionPoints = processingNode.instructions.asSequence()
            .filter { isBeforeSuspendMarker(it) }
            .flatMap { findMeaningfulSuccs(it).asSequence() }
            .filter { it is MethodInsnNode }

        val toReplace = hashSetOf<AbstractInsnNode>()
        for (suspensionPoint in suspensionPoints) {
            assert(suspensionPoint is MethodInsnNode) {
                "suspensionPoint shall be MethodInsnNode, but instead $suspensionPoint"
            }
            suspensionPoint as MethodInsnNode
            assert(Type.getReturnType(suspensionPoint.desc) == OBJECT_TYPE) {
                "suspensionPoint shall return $OBJECT_TYPE, but returns ${Type.getReturnType(suspensionPoint.desc)}"
            }
            val frame = sources[processingNode.instructions.indexOf(suspensionPoint)] ?: continue
            val paramTypes = Type.getArgumentTypes(suspensionPoint.desc)
            if (suspensionPoint.name.endsWith(JvmAbi.DEFAULT_PARAMS_IMPL_SUFFIX)) {
                // Expected pattern here:
                //     ALOAD 0
                //     (ICONST or other integers creating instruction)
                //     (ACONST_NULL or ALOAD)
                //     ICONST_0
                //     INVOKESTATIC InlineMarker.mark
                //     INVOKE* suspendingFunction$default(..., Continuation;ILjava/lang/Object)Ljava/lang/Object;
                assert(paramTypes.size >= 3) {
                    "${suspensionPoint.name}${suspensionPoint.desc} shall have 3+ parameters"
                }
            } else {
                // Expected pattern here:
                //     ALOAD 0
                //     ICONST_0
                //     INVOKESTATIC InlineMarker.mark
                //     INVOKE* suspendingFunction(..., Continuation;)Ljava/lang/Object;
                assert(paramTypes.isNotEmpty()) {
                    "${suspensionPoint.name}${suspensionPoint.desc} shall have 1+ parameters"
                }
            }

            for ((index, param) in paramTypes.reversed().withIndex()) {
                if (param != CONTINUATION_ASM_TYPE && param != OBJECT_TYPE) continue
                val sourceIndices = (frame.getStack(frame.stackSize - index - 1) as? Aload0BasicValue)?.indices ?: continue
                for (sourceIndex in sourceIndices) {
                    val src = processingNode.instructions[sourceIndex]
                    if (src in aload0s) {
                        toReplace.add(src)
                    }
                }
            }
        }

        // Expected pattern here:
        //     ALOAD 0
        //     ASTORE N
        // This pattern may occur after multiple inlines
        // Note, that this is not a suspension point, thus we check it separately
        toReplace.addAll(aload0s.filter { it.next?.opcode == Opcodes.ASTORE })
        // Expected pattern here:
        //     ALOAD 0
        //     INVOKEINTERFACE kotlin/jvm/functions/FunctionN.invoke (...,Ljava/lang/Object;)Ljava/lang/Object;
        toReplace.addAll(aload0s.filter { isLambdaCall(it.next) })
        replaceContinuationsWithFakeOnes(toReplace, processingNode)
    }

    private fun isLambdaCall(invoke: AbstractInsnNode?): Boolean {
        if (invoke?.opcode != Opcodes.INVOKEINTERFACE) return false
        invoke as MethodInsnNode
        if (!invoke.owner.startsWith("kotlin/jvm/functions/Function")) return false
        if (invoke.name != "invoke") return false
        if (Type.getReturnType(invoke.desc) != OBJECT_TYPE) return false
        return Type.getArgumentTypes(invoke.desc).let { it.isNotEmpty() && it.last() == OBJECT_TYPE }
    }

    private fun replaceContinuationsWithFakeOnes(
        continuations: Collection<AbstractInsnNode>,
        node: MethodNode
    ) {
        for (toReplace in continuations) {
            insertNodeBefore(createFakeContinuationMethodNodeForInline(), node, toReplace)
            node.instructions.remove(toReplace)
        }
    }

    private fun preprocessNodeBeforeInline(node: MethodNode, returnLabels: Map<String, Label?>) {
        try {
            InplaceArgumentsMethodTransformer().transform("fake", node)
            FixStackWithLabelNormalizationMethodTransformer().transform("fake", node)
            TemporaryVariablesEliminationTransformer(inliningContext.state).transform("fake", node)
        } catch (e: Throwable) {
            throw wrapException(e, node, "couldn't inline method call")
        }

        if (shouldPreprocessApiVersionCalls) {
            val targetApiVersion = inliningContext.state.languageVersionSettings.apiVersion
            ApiVersionCallsPreprocessingMethodTransformer(targetApiVersion).transform("fake", node)
        }

        removeFakeVariablesInitializationIfPresent(node)

        val frames = FastStackAnalyzer("<fake>", node, FixStackInterpreter()).analyze()

        val localReturnsNormalizer = LocalReturnsNormalizer()

        for ((index, insnNode) in node.instructions.toArray().withIndex()) {
            val frame = frames[index] ?: continue
            // Don't care about dead code, it will be eliminated

            if (!isReturnOpcode(insnNode.opcode)) continue

            // TODO extract isLocalReturn / isNonLocalReturn, see processReturns
            val labelName = getMarkedReturnLabelOrNull(insnNode)
            if (labelName == null) {
                localReturnsNormalizer.addLocalReturnToTransform(insnNode, insnNode, frame)
            } else if (labelName in returnLabels) {
                localReturnsNormalizer.addLocalReturnToTransform(insnNode, insnNode.previous, frame)
            }
        }

        localReturnsNormalizer.transform(node)
    }

    private fun removeFakeVariablesInitializationIfPresent(node: MethodNode) {
        // Before 1.6, we generated fake variable initialization instructions
        //      ICONST_0
        //      ISTORE x
        // for all inline functions. Original intent was to mark inline function body for the debugger with corresponding LVT entry.
        // However, for @InlineOnly functions corresponding LVT entries were not copied (assuming that nobody is actually debugging
        // @InlineOnly functions).
        // Since 1.6, we no longer generate fake variables for @InlineOnly functions
        // Here we erase fake variable initialization for @InlineOnly functions inlined into existing bytecode (e.g., inline function
        // inside third-party library).
        // We consider a sequence of instructions 'ICONST_0; ISTORE x' a fake variable initialization if the corresponding variable 'x'
        // is not used in the bytecode (see below).

        val insnArray = node.instructions.toArray()

        // Very conservative variable usage check.
        // Here we look at integer variables only (this includes integral primitive types: byte, char, short, boolean).
        // Variable is considered "used" if:
        //  - it's loaded with ILOAD instruction
        //  - it's incremented with IINC instruction
        //  - there's a local variable table entry for this variable
        val usedIntegerVar = BooleanArray(node.maxLocals)
        for (insn in insnArray) {
            if (insn.type == AbstractInsnNode.VAR_INSN && insn.opcode == Opcodes.ILOAD) {
                usedIntegerVar[(insn as VarInsnNode).`var`] = true
            } else if (insn.type == AbstractInsnNode.IINC_INSN) {
                usedIntegerVar[(insn as IincInsnNode).`var`] = true
            }
        }
        for (localVariable in node.localVariables) {
            val d0 = localVariable.desc[0]
            // byte || char || short || int || boolean
            if (d0 == 'B' || d0 == 'C' || d0 == 'S' || d0 == 'I' || d0 == 'Z') {
                usedIntegerVar[localVariable.index] = true
            }
        }

        // Looking for sequences of instructions:
        //  p0: ICONST_0
        //  p1: ISTORE x
        //  p2: <label>
        // If variable 'x' is not "used" (see above), remove p0 and p1 instructions.
        var changes = false
        for (p0 in insnArray) {
            if (p0.opcode != Opcodes.ICONST_0) continue

            val p1 = p0.next ?: break
            if (p1.opcode != Opcodes.ISTORE) continue

            val p2 = p1.next ?: break
            if (p2.type != AbstractInsnNode.LABEL) continue

            val varIndex = (p1 as VarInsnNode).`var`
            if (!usedIntegerVar[varIndex]) {
                changes = true
                node.instructions.remove(p0)
                node.instructions.remove(p1)
            }
        }

        if (changes) {
            // If we removed some instructions, some TCBs could (in theory) become empty.
            // Remove empty TCBs if there are any.
            node.removeEmptyCatchBlocks()
        }
    }

    private fun isAnonymousClassThatMustBeRegenerated(type: Type?): Boolean {
        if (type == null || type.sort != Type.OBJECT) return false
        return inliningContext.isRegeneratedAnonymousObject(type.internalName)
    }

    private fun buildConstructorInvocation(
        anonymousType: String,
        desc: String,
        lambdaMapping: Map<Int, FunctionalArgument>,
        needReification: Boolean,
        capturesAnonymousObjectThatMustBeRegenerated: Boolean
    ): AnonymousObjectTransformationInfo {
        // In objects inside non-default inline lambdas, all reified type parameters are free (not from the function
        // we're inlining into) so there's nothing to reify:
        //
        //     inline fun <reified T> f(x: () -> KClass<T> = { { T::class }() }) = x()
        //     fun a() = f<Int>()
        //     fun b() = f<Int> { { Int::class }() } // non-default lambda
        //     inline fun <reified V> c() = f<V> { { V::class }() }
        //
        // -- in a(), the default inline lambda captures T so a regeneration is needed; but in b() and c(), the non-default
        // inline lambda cannot possibly reference it, while V is not yet bound so regenerating the object while inlining
        // the lambda into f() is pointless.
        val inNonDefaultLambda = inliningContext.isInliningLambda && inliningContext.lambdaInfo !is DefaultLambda
        return AnonymousObjectTransformationInfo(
            anonymousType, needReification && !inNonDefaultLambda, lambdaMapping,
            inliningContext.classRegeneration,
            isAlreadyRegenerated(anonymousType),
            desc,
            false,
            inliningContext.nameGenerator,
            capturesAnonymousObjectThatMustBeRegenerated
        )
    }

    private fun isAlreadyRegenerated(owner: String): Boolean {
        return inliningContext.typeRemapper.hasNoAdditionalMapping(owner)
    }

    internal fun getFunctionalArgumentIfExists(insnNode: AbstractInsnNode): FunctionalArgument? {
        return when {
            insnNode.opcode == Opcodes.ALOAD ->
                getFunctionalArgumentIfExists((insnNode as VarInsnNode).`var`)
            insnNode is FieldInsnNode && insnNode.name.startsWith(CAPTURED_FIELD_FOLD_PREFIX) ->
                findCapturedField(insnNode, nodeRemapper).functionalArgument
            insnNode is FieldInsnNode && inliningContext.root.sourceCompilerForInline.isSuspendLambdaCapturedByOuterObjectOrLambda(insnNode.name) ->
                NonInlineArgumentForInlineSuspendParameter.INLINE_LAMBDA_AS_VARIABLE
            else ->
                null
        }
    }

    private fun getFunctionalArgumentIfExists(varIndex: Int): FunctionalArgument? {
        if (varIndex < parameters.argsSizeOnStack) {
            return parameters.getParameterByDeclarationSlot(varIndex).functionalArgument
        }
        return null
    }

    private fun transformCaptured(node: MethodNode) {
        if (nodeRemapper.isRoot) {
            return
        }

        if (inliningContext.isInliningLambda && inliningContext.lambdaInfo is IrExpressionLambda && !inliningContext.parent!!.isInliningLambda) {
            val capturedVars = inliningContext.lambdaInfo.capturedVars
            var offset = parameters.realParametersSizeOnStack
            val map = capturedVars.associate {
                offset to it.also { offset += it.type.size }
            }

            var cur: AbstractInsnNode? = node.instructions.first
            while (cur != null) {
                if (cur is VarInsnNode && cur.opcode in Opcodes.ILOAD..Opcodes.ALOAD && map.contains(cur.`var`)) {
                    val varIndex = cur.`var`
                    val capturedParamDesc = map[varIndex]!!

                    val newIns = FieldInsnNode(
                        Opcodes.GETSTATIC,
                        capturedParamDesc.containingLambdaName,
                        foldName(capturedParamDesc.fieldName),
                        capturedParamDesc.type.descriptor
                    )
                    node.instructions.insertBefore(cur, newIns)
                    node.instructions.remove(cur)
                    cur = newIns
                }
                cur = cur.next
            }
        }

        // Fold all captured variables access chains
        //          ALOAD 0
        //          [ALOAD this$0]*
        //          GETFIELD $captured
        //  to GETFIELD $$$$captured
        // On future decoding this field could be inlined or unfolded to another field access chain
        // (this chain could differ cause some of this$0 could be inlined)
        var cur: AbstractInsnNode? = node.instructions.first
        while (cur != null) {
            if (cur is VarInsnNode && cur.opcode == Opcodes.ALOAD) {
                val varIndex = cur.`var`
                if (varIndex == 0 || nodeRemapper.shouldProcessNonAload0FieldAccessChains()) {
                    val accessChain = getCapturedFieldAccessChain((cur as VarInsnNode?)!!)
                    val insnNode = nodeRemapper.foldFieldAccessChainIfNeeded(accessChain, node)
                    if (insnNode != null) {
                        cur = insnNode
                    }
                }
            }
            cur = cur.next
        }
    }

    @Suppress("SameParameterValue")
    private fun wrapException(originalException: Throwable, node: MethodNode, errorSuffix: String): RuntimeException {
        return if (originalException is InlineException) {
            InlineException("$errorPrefix: $errorSuffix", originalException)
        } else {
            InlineException("$errorPrefix: $errorSuffix\nCause: ${node.nodeText}", originalException)
        }
    }

    private class LocalReturnsNormalizer {
        private class LocalReturn(
            private val returnInsn: AbstractInsnNode,
            private val insertBeforeInsn: AbstractInsnNode,
            private val frame: Frame<FixStackValue>
        ) {

            fun transform(insnList: InsnList, returnVariableIndex: Int) {
                val isReturnWithValue = returnInsn.opcode != Opcodes.RETURN

                val expectedStackSize = if (isReturnWithValue) 1 else 0
                val actualStackSize = frame.stackSize
                if (expectedStackSize == actualStackSize) return

                var stackSize = actualStackSize
                val topValue = frame.getStack(stackSize - 1)
                if (isReturnWithValue) {
                    insnList.insertBefore(insertBeforeInsn, VarInsnNode(topValue.storeOpcode, returnVariableIndex))
                    stackSize--
                }

                while (stackSize > 0) {
                    insnList.insertBefore(insertBeforeInsn, InsnNode(frame.getStack(stackSize - 1).popOpcode))
                    stackSize--
                }

                if (isReturnWithValue) {
                    insnList.insertBefore(insertBeforeInsn, VarInsnNode(topValue.loadOpcode, returnVariableIndex))
                }
            }
        }

        private val localReturns = SmartList<LocalReturn>()

        private var returnVariableSize = 0
        private var returnOpcode = -1

        fun addLocalReturnToTransform(
            returnInsn: AbstractInsnNode,
            insertBeforeInsn: AbstractInsnNode,
            sourceValueFrame: Frame<FixStackValue>
        ) {
            assert(isReturnOpcode(returnInsn.opcode)) { "return instruction expected" }
            assert(returnOpcode < 0 || returnOpcode == returnInsn.opcode) { "Return op should be " + Printer.OPCODES[returnOpcode] + ", got " + Printer.OPCODES[returnInsn.opcode] }
            returnOpcode = returnInsn.opcode

            localReturns.add(LocalReturn(returnInsn, insertBeforeInsn, sourceValueFrame))

            if (returnInsn.opcode != Opcodes.RETURN) {
                returnVariableSize = if (returnInsn.opcode == Opcodes.LRETURN || returnInsn.opcode == Opcodes.DRETURN) 2 else 1
            }
        }

        fun transform(methodNode: MethodNode) {
            var returnVariableIndex = -1
            if (returnVariableSize > 0) {
                returnVariableIndex = methodNode.maxLocals
                methodNode.maxLocals += returnVariableSize
            }

            for (localReturn in localReturns) {
                localReturn.transform(methodNode.instructions, returnVariableIndex)
            }
        }
    }

    //Place to insert finally blocks from try blocks that wraps inline fun call
    class PointForExternalFinallyBlocks(
        @JvmField val beforeIns: AbstractInsnNode,
        @JvmField val returnType: Type,
        @JvmField val finallyIntervalEnd: LabelNode,
        @JvmField val jumpTarget: Label?
    )

    companion object {

        @JvmStatic
        fun findCapturedField(node: FieldInsnNode, fieldRemapper: FieldRemapper): CapturedParamInfo {
            assert(node.name.startsWith(CAPTURED_FIELD_FOLD_PREFIX)) {
                "Captured field template should start with $CAPTURED_FIELD_FOLD_PREFIX prefix"
            }
            val fin = FieldInsnNode(node.opcode, node.owner, node.name.substring(3), node.desc)
            return fieldRemapper.findField(fin) ?: throw IllegalStateException(
                "Couldn't find captured field ${node.owner}.${node.name} in ${fieldRemapper.originalLambdaInternalName}"
            )
        }

        //remove next template:
        //      aload x
        //      LDC paramName
        //      INTRINSICS_CLASS_NAME.checkParameterIsNotNull/checkNotNullParameter(...)
        private fun removeClosureAssertions(node: MethodNode) {
            val toDelete = arrayListOf<AbstractInsnNode>()
            InsnSequence(node.instructions).filterIsInstance<MethodInsnNode>().forEach { methodInsnNode ->
                if (methodInsnNode.isCheckParameterIsNotNull()) {
                    val prev = methodInsnNode.previous
                    assert(Opcodes.LDC == prev?.opcode) { "'${methodInsnNode.name}' should go after LDC but $prev" }
                    val prevPev = methodInsnNode.previous.previous
                    assert(Opcodes.ALOAD == prevPev?.opcode) { "'${methodInsnNode.name}' should be invoked on local var, but $prev" }

                    toDelete.add(prevPev)
                    toDelete.add(prev)
                    toDelete.add(methodInsnNode)
                }
            }

            node.remove(toDelete)
        }

        private fun transformFinallyDeepIndex(node: MethodNode, finallyDeepShift: Int) {
            if (finallyDeepShift == 0) {
                return
            }

            var cur: AbstractInsnNode? = node.instructions.first
            while (cur != null) {
                if (cur is MethodInsnNode && isFinallyMarker(cur)) {
                    val constant = cur.previous
                    val curDeep = getConstant(constant)
                    node.instructions.insert(constant, LdcInsnNode(curDeep + finallyDeepShift))
                    node.instructions.remove(constant)
                }
                cur = cur.next
            }
        }

        private fun getCapturedFieldAccessChain(aload0: VarInsnNode): List<AbstractInsnNode> {
            val lambdaAccessChain = mutableListOf<AbstractInsnNode>(aload0).apply {
                addAll(InsnSequence(aload0.next, null).filter { it.isMeaningful }.takeWhile { insnNode ->
                    insnNode is FieldInsnNode && AsmUtil.CAPTURED_THIS_FIELD == insnNode.name
                }.toList())
            }

            return lambdaAccessChain.apply {
                last().getNextMeaningful().takeIf { insn -> insn is FieldInsnNode }?.also {
                    //captured field access
                        insn ->
                    add(insn)
                }
            }
        }

        //process local and global returns (local substituted with goto end-label global kept unchanged)
        @JvmStatic
        fun processReturns(
            node: MethodNode, returnLabels: Map<String, Label?>, endLabel: Label?
        ): List<PointForExternalFinallyBlocks> {
            val result = ArrayList<PointForExternalFinallyBlocks>()
            val instructions = node.instructions
            var insnNode: AbstractInsnNode? = instructions.first
            while (insnNode != null) {
                if (isReturnOpcode(insnNode.opcode)) {
                    val labelName = getMarkedReturnLabelOrNull(insnNode)
                    val returnType = getReturnType(insnNode.opcode)

                    val isLocalReturn = labelName == null || labelName in returnLabels
                    val jumpTarget = returnLabels[labelName] ?: endLabel

                    if (isLocalReturn && labelName != null) {
                        // remove non-local return flag
                        instructions.remove(insnNode.previous)
                    }

                    if (isLocalReturn && jumpTarget != null) {
                        val jumpInsnNode = JumpInsnNode(Opcodes.GOTO, jumpTarget.info as LabelNode)
                        instructions.insertBefore(insnNode, InsnNode(Opcodes.NOP))
                        if (jumpTarget != endLabel) {
                            instructions.insertBefore(insnNode, PseudoInsn.FIX_STACK_BEFORE_JUMP.createInsnNode())
                        }
                        instructions.insertBefore(insnNode, jumpInsnNode)
                        instructions.remove(insnNode)
                        insnNode = jumpInsnNode
                    }

                    val label = LabelNode()
                    instructions.insert(insnNode, label)
                    // generate finally blocks before the non-local return flag or the stack fixup pseudo instruction
                    val finallyInsertionPoint = if (isLocalReturn && jumpTarget == endLabel) insnNode else insnNode.previous
                    result.add(PointForExternalFinallyBlocks(finallyInsertionPoint, returnType, label, jumpTarget))
                }
                insnNode = insnNode.next
            }
            return result
        }
    }
}

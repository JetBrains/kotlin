/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.kotlin.backend.jvm.codegen.IrExpressionLambda
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.ClosureCodegen
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.coroutines.continuationAsmType
import org.jetbrains.kotlin.codegen.coroutines.getOrCreateJvmSuspendFunctionView
import org.jetbrains.kotlin.codegen.inline.FieldRemapper.Companion.foldName
import org.jetbrains.kotlin.codegen.intrinsics.IntrinsicMethods
import org.jetbrains.kotlin.codegen.optimization.ApiVersionCallsPreprocessingMethodTransformer
import org.jetbrains.kotlin.codegen.optimization.FixStackWithLabelNormalizationMethodTransformer
import org.jetbrains.kotlin.codegen.optimization.common.ControlFlowGraph
import org.jetbrains.kotlin.codegen.optimization.common.InsnSequence
import org.jetbrains.kotlin.codegen.optimization.common.asSequence
import org.jetbrains.kotlin.codegen.optimization.common.isMeaningful
import org.jetbrains.kotlin.codegen.optimization.fixStack.peek
import org.jetbrains.kotlin.codegen.optimization.fixStack.top
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.SmartSet
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.commons.RemappingMethodAdapter
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.tree.analysis.*
import org.jetbrains.org.objectweb.asm.util.Printer
import java.util.*

class MethodInliner(
        private val node: MethodNode,
        private val parameters: Parameters,
        private val inliningContext: InliningContext,
        private val nodeRemapper: FieldRemapper,
        private val isSameModule: Boolean,
        private val errorPrefix: String,
        private val sourceMapper: SourceMapper,
        private val inlineCallSiteInfo: InlineCallSiteInfo,
        private val inlineOnlySmapSkipper: InlineOnlySmapSkipper?, //non null only for root
        private val shouldPreprocessApiVersionCalls: Boolean = false
) {
    private val typeMapper = inliningContext.state.typeMapper
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
            labelOwner: LabelOwner
    ): InlineResult {
        return doInline(adapter, remapper, remapReturn, labelOwner, 0)
    }

    private fun recordTransformation(info: TransformationInfo) {
        if (!inliningContext.isInliningLambda) {
            inliningContext.root.state.globalInlineContext.recordTypeFromInlineFunction(info.oldClassName)
        }
        transformations.add(info)
    }

    private fun doInline(
            adapter: MethodVisitor,
            remapper: LocalVarRemapper,
            remapReturn: Boolean,
            labelOwner: LabelOwner,
            finallyDeepShift: Int
    ): InlineResult {
        //analyze body
        var transformedNode = markPlacesForInlineAndRemoveInlinable(node, labelOwner, finallyDeepShift)
        if (inliningContext.isInliningLambda && isDefaultLambdaWithReification(inliningContext.lambdaInfo!!)) {
            //TODO maybe move reification in one place
            inliningContext.root.inlineMethodReifier.reifyInstructions(transformedNode)
        }

        //substitute returns with "goto end" instruction to keep non local returns in lambdas
        val end = Label()
        val isTransformingAnonymousObject = nodeRemapper is RegeneratedLambdaFieldRemapper
        transformedNode = doInline(transformedNode)
        if (!isTransformingAnonymousObject) {
            //don't remove assertion in transformed anonymous object
            removeClosureAssertions(transformedNode)
        }
        transformedNode.instructions.resetLabels()

        val resultNode = MethodNode(
                API, transformedNode.access, transformedNode.name, transformedNode.desc,
                transformedNode.signature, transformedNode.exceptions?.toTypedArray()
        )
        val visitor = RemapVisitor(
                resultNode, remapper, nodeRemapper,
                /*copy annotation and attributes*/
                isTransformingAnonymousObject
        )
        try {
            transformedNode.accept(visitor)
        }
        catch (e: Throwable) {
            throw wrapException(e, transformedNode, "couldn't inline method call")
        }

        resultNode.visitLabel(end)

        if (inliningContext.isRoot) {
            val remapValue = remapper.remap(parameters.argsSizeOnStack + 1).value
            InternalFinallyBlockInliner.processInlineFunFinallyBlocks(
                    resultNode, lambdasFinallyBlocks, (remapValue as StackValue.Local).index
            )
        }

        processReturns(resultNode, labelOwner, remapReturn, end)

        //flush transformed node to output
        AsmUtil.resetLabelInfos(resultNode)
        resultNode.accept(MethodBodyVisitor(adapter, true))

        sourceMapper.endMapping()
        return result
    }

    private fun doInline(node: MethodNode): MethodNode {
        val currentInvokes = LinkedList(invokeCalls)

        val resultNode = MethodNode(node.access, node.name, node.desc, node.signature, null)

        val iterator = transformations.iterator()

        val remapper = TypeRemapper.createFrom(currentTypeMapping)
        val remappingMethodAdapter = RemappingMethodAdapter(
                resultNode.access,
                resultNode.desc,
                resultNode,
                AsmTypeRemapper(remapper, result)
        )

        val markerShift = calcMarkerShift(parameters, node)
        val lambdaInliner = object : InlineAdapter(remappingMethodAdapter, parameters.argsSizeOnStack, sourceMapper) {
            private var transformationInfo: TransformationInfo? = null

            private fun handleAnonymousObjectRegeneration() {
                transformationInfo = iterator.next()

                val oldClassName = transformationInfo!!.oldClassName
                if (transformationInfo!!.shouldRegenerate(isSameModule)) {
                    //TODO: need poping of type but what to do with local funs???
                    val newClassName = transformationInfo!!.newClassName
                    remapper.addMapping(oldClassName, newClassName)

                    val childInliningContext = inliningContext.subInlineWithClassRegeneration(
                            inliningContext.nameGenerator,
                            currentTypeMapping,
                            inlineCallSiteInfo
                    )
                    val transformer = transformationInfo!!.createTransformer(
                        childInliningContext,
                        isSameModule,
                        findFakeContinuationConstructorClassName(node)
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
                }
                else if (!transformationInfo!!.wasAlreadyRegenerated) {
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
                    val info = invokeCall.lambdaInfo

                    if (info == null) {
                        //noninlinable lambda
                        super.visitMethodInsn(opcode, owner, name, desc, itf)
                        return
                    }

                    // in case of inlining suspend lambda reference as ordinary parameter of inline function:
                    //   suspend fun foo (...) ...
                    //   inline fun inlineMe(c: (...) -> ...) ...
                    //   builder {
                    //     inlineMe(::foo)
                    //   }
                    // we should create additional parameter for continuation.
                    var coroutineDesc = desc
                    val actualInvokeDescriptor: FunctionDescriptor
                    if (info.invokeMethodDescriptor.isSuspend) {
                        val coroutineInvokeMethodDescriptor = getOrCreateJvmSuspendFunctionView(
                            info.invokeMethodDescriptor,
                            inliningContext.state
                        )
                        actualInvokeDescriptor = coroutineInvokeMethodDescriptor
                        val coroutineInvokeDesc = typeMapper.mapAsmMethod(coroutineInvokeMethodDescriptor).descriptor
                        // And here we expect invoke(...Ljava/lang/Object;) be replaced with invoke(...Lkotlin/coroutines/Continuation;)
                        // if this does not happen, insert fake continuation, since we could not have one yet.
                        val argumentTypes = Type.getArgumentTypes(desc)
                        if (Type.getArgumentTypes(coroutineInvokeDesc).size != argumentTypes.size) {
                            addFakeContinuationMarker(this)
                            coroutineDesc = Type.getMethodDescriptor(Type.getReturnType(desc), *argumentTypes, AsmTypes.OBJECT_TYPE)
                        }
                    } else {
                        actualInvokeDescriptor = info.invokeMethodDescriptor
                    }

                    val valueParameters =
                        listOfNotNull(actualInvokeDescriptor.extensionReceiverParameter) + actualInvokeDescriptor.valueParameters

                    val erasedInvokeFunction = ClosureCodegen.getErasedInvokeFunction(actualInvokeDescriptor)
                    val invokeParameters = erasedInvokeFunction.valueParameters

                    val valueParamShift = Math.max(nextLocalIndex, markerShift)//NB: don't inline cause it changes
                    putStackValuesIntoLocalsForLambdaOnInvoke(
                        listOf(*info.invokeMethod.argumentTypes), valueParameters, invokeParameters, valueParamShift, this, coroutineDesc
                    )

                    if (invokeCall.lambdaInfo.invokeMethodDescriptor.valueParameters.isEmpty()) {
                        // There won't be no parameters processing and line call can be left without actual instructions.
                        // Note: if function is called on the line with other instructions like 1 + foo(), 'nop' will still be generated.
                        visitInsn(Opcodes.NOP)
                    }

                    addInlineMarker(this, true)
                    val lambdaParameters = info.addAllParameters(nodeRemapper)

                    val newCapturedRemapper = InlinedLambdaRemapper(
                            info.lambdaClassType.internalName, nodeRemapper, lambdaParameters,
                            info is DefaultLambda && info.isBoundCallableReference
                    )

                    setLambdaInlining(true)
                    val lambdaSMAP = info.node.classSMAP

                    val childSourceMapper =
                            if (inliningContext.classRegeneration && !inliningContext.isInliningLambda)
                                NestedSourceMapper(sourceMapper, lambdaSMAP.intervals, lambdaSMAP.sourceInfo)
                            else if (info is DefaultLambda) {
                                NestedSourceMapper(sourceMapper.parent!!, lambdaSMAP.intervals, lambdaSMAP.sourceInfo)
                            }
                            else InlineLambdaSourceMapper(sourceMapper.parent!!, info.node)

                    val inliner = MethodInliner(
                            info.node.node, lambdaParameters, inliningContext.subInlineLambda(info),
                            newCapturedRemapper,
                            if (info is DefaultLambda) isSameModule else true /*cause all nested objects in same module as lambda*/,
                            "Lambda inlining " + info.lambdaClassType.internalName,
                            childSourceMapper, inlineCallSiteInfo, null
                    )

                    val varRemapper = LocalVarRemapper(lambdaParameters, valueParamShift)
                    //TODO add skipped this and receiver
                    val lambdaResult = inliner.doInline(this.mv, varRemapper, true, info, invokeCall.finallyDepthShift)
                    result.mergeWithNotChangeInfo(lambdaResult)
                    result.reifiedTypeParametersUsages.mergeAll(lambdaResult.reifiedTypeParametersUsages)

                    val bridge = typeMapper.mapAsmMethod(erasedInvokeFunction)
                    StackValue
                        .onStack(info.invokeMethod.returnType, info.invokeMethodDescriptor.returnType)
                        .put(bridge.returnType, erasedInvokeFunction.returnType, this)
                    setLambdaInlining(false)
                    addInlineMarker(this, false)
                    childSourceMapper.endMapping()
                    inlineOnlySmapSkipper?.markCallSiteLineNumber(remappingMethodAdapter)
                }
                else if (isAnonymousConstructorCall(owner, name)) { //TODO add method
                    //TODO add proper message
                    assert(transformationInfo is AnonymousObjectTransformationInfo) {
                        "<init> call doesn't correspond to object transformation info for '$owner.$name': $transformationInfo"
                    }
                    val parent = inliningContext.parent
                    val shouldRegenerate = transformationInfo!!.shouldRegenerate(isSameModule)
                    val isContinuation = parent != null && parent.isContinuation
                    if (shouldRegenerate || isContinuation) {
                        assert(shouldRegenerate || inlineCallSiteInfo.ownerClassName == transformationInfo!!.oldClassName) { "Only coroutines can call their own constructors" }

                        //put additional captured parameters on stack
                        var info = transformationInfo as AnonymousObjectTransformationInfo

                        val oldInfo = inliningContext.findAnonymousObjectTransformationInfo(owner)
                        if (oldInfo != null && isContinuation) {
                            info = oldInfo
                        }

                        val isContinuationCreate = isContinuation && oldInfo != null && resultNode.name == "create" &&
                                resultNode.desc.endsWith(")" + languageVersionSettings.continuationAsmType().descriptor)

                        for (capturedParamDesc in info.allRecapturedParameters) {
                            if (capturedParamDesc.fieldName == AsmUtil.THIS && isContinuationCreate) {
                                // Common inliner logic doesn't support cases when transforming anonymous object can
                                // be instantiated by itself.
                                // To support such cases workaround with 'oldInfo' is used.
                                // But it corresponds to outer context and a bit inapplicable for nested 'create' method context.
                                // 'This' in outer context corresponds to outer instance in current
                                visitFieldInsn(
                                    Opcodes.GETSTATIC, owner,
                                    CAPTURED_FIELD_FOLD_PREFIX + AsmUtil.CAPTURED_THIS_FIELD, capturedParamDesc.type.descriptor
                                )
                            } else {
                                visitFieldInsn(
                                    Opcodes.GETSTATIC, capturedParamDesc.containingLambdaName,
                                    CAPTURED_FIELD_FOLD_PREFIX + capturedParamDesc.fieldName, capturedParamDesc.type.descriptor
                                )
                            }
                        }
                        super.visitMethodInsn(opcode, info.newClassName, name, info.newConstructorDescriptor, itf)

                        //TODO: add new inner class also for other contexts
                        if (inliningContext.parent is RegeneratedClassContext) {
                            inliningContext.parent.typeRemapper.addAdditionalMappings(
                                transformationInfo!!.oldClassName, transformationInfo!!.newClassName
                            )
                        }

                        transformationInfo = null
                    }
                    else {
                        super.visitMethodInsn(opcode, owner, name, desc, itf)
                    }
                }
                else if ((!inliningContext.isInliningLambda || isDefaultLambdaWithReification(inliningContext.lambdaInfo!!)) &&
                    ReifiedTypeInliner.isNeedClassReificationMarker(MethodInsnNode(opcode, owner, name, desc, false))) {
                    //we shouldn't process here content of inlining lambda it should be reified at external level except default lambdas
                }
                else {
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

        AsmUtil.resetLabelInfos(node)
        node.accept(lambdaInliner)

        return resultNode
    }

    private fun isDefaultLambdaWithReification(lambdaInfo: LambdaInfo) =
            lambdaInfo is DefaultLambda && lambdaInfo.needReification

    private fun prepareNode(node: MethodNode, finallyDeepShift: Int): MethodNode {
        node.instructions.resetLabels()

        val capturedParamsSize = parameters.capturedParametersSizeOnStack
        val realParametersSize = parameters.realParametersSizeOnStack
        val transformedNode = MethodNode(
            API, node.access, node.name,
            Type.getMethodDescriptor(Type.getReturnType(node.desc), *(Type.getArgumentTypes(node.desc) + parameters.capturedTypes)),
            node.signature, node.exceptions?.toTypedArray()
        )

        val transformationVisitor = object : InlineMethodInstructionAdapter(transformedNode) {
            private val GENERATE_DEBUG_INFO = GENERATE_SMAP && inlineOnlySmapSkipper == null

            private val isInliningLambda = nodeRemapper.isInsideInliningLambda

            private fun getNewIndex(`var`: Int): Int {
                if (inliningContext.isInliningLambda && inliningContext.lambdaInfo is IrExpressionLambda) {
                    if (`var` < parameters.argsSizeOnStack) {
                        if (`var` < capturedParamsSize) {
                            return `var` + realParametersSize
                        }
                        else {
                            return `var` - capturedParamsSize
                        }
                    }
                    return `var`
                }
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
                    val lambda = getLambdaIfExists(index) as DefaultLambda
                    lambda.parameterOffsetsInDefault.zip(lambda.capturedVars).asReversed().forEach {
                        (_, captured) ->
                        val originalBoundReceiverType = lambda.originalBoundReceiverType
                        if (lambda.isBoundCallableReference && AsmUtil.isPrimitive(originalBoundReceiverType)) {
                            StackValue.onStack(originalBoundReceiverType!!).put(captured.type, InstructionAdapter(this))
                        }
                        super.visitFieldInsn(
                                Opcodes.PUTSTATIC, captured.containingLambdaName, CAPTURED_FIELD_FOLD_PREFIX + captured.fieldName, captured.type.descriptor
                        )
                    }
                }
                else {
                    super.visitMethodInsn(opcode, owner, name, desc, itf)
                }
            }

            override fun visitLocalVariable(
                    name: String, desc: String, signature: String?, start: Label, end: Label, index: Int
            ) {
                if (isInliningLambda || GENERATE_DEBUG_INFO) {
                    val varSuffix = if (inliningContext.isRoot && !isFakeLocalVariableForInline(name)) INLINE_FUN_VAR_SUFFIX else ""
                    val varName = if (!varSuffix.isEmpty() && name == "this") name + "_" else name
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
            node: MethodNode, labelOwner: LabelOwner, finallyDeepShift: Int
    ): MethodNode {
        val processingNode = prepareNode(node, finallyDeepShift)

        preprocessNodeBeforeInline(processingNode, labelOwner)

        replaceContinuationAccessesWithFakeContinuationsIfNeeded(processingNode)

        val sources = analyzeMethodNodeBeforeInline(processingNode)

        val toDelete = SmartSet.create<AbstractInsnNode>()
        val instructions = processingNode.instructions

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
                            val lambdaInfo = getLambdaIfExistsAndMarkInstructions(sourceValue, true, instructions, sources, toDelete)
                            invokeCalls.add(InvokeCall(lambdaInfo, currentFinallyDeep))
                        }
                        else if (isSamWrapperConstructorCall(owner, name)) {
                            recordTransformation(SamWrapperTransformationInfo(owner, inliningContext, isAlreadyRegenerated(owner)))
                        }
                        else if (isAnonymousConstructorCall(owner, name)) {
                            val lambdaMapping = HashMap<Int, LambdaInfo>()

                            var offset = 0
                            var capturesAnonymousObjectThatMustBeRegenerated = false
                            for (i in 0 until paramCount) {
                                val sourceValue = frame.getStack(firstParameterIndex + i)
                                val lambdaInfo = getLambdaIfExistsAndMarkInstructions(sourceValue, false, instructions, sources, toDelete
                                )
                                if (lambdaInfo != null) {
                                    lambdaMapping.put(offset, lambdaInfo)
                                }
                                else if (i < argTypes.size && isAnonymousClassThatMustBeRegenerated(argTypes[i])) {
                                    capturesAnonymousObjectThatMustBeRegenerated = true
                                }

                                offset += if (i == 0) 1 else argTypes[i - 1].size
                            }

                            recordTransformation(
                                    buildConstructorInvocation(
                                            owner, cur.desc, lambdaMapping, awaitClassReification, capturesAnonymousObjectThatMustBeRegenerated
                                    )
                            )
                            awaitClassReification = false
                        }
                        else if (inliningContext.isInliningLambda && ReifiedTypeInliner.isOperationReifiedMarker(cur)) {
                            val reificationArgument = cur.reificationArgument
                            val parameterName = reificationArgument!!.parameterName
                            result.reifiedTypeParametersUsages.addUsedReifiedParameter(parameterName)
                        }
                    }

                    cur.opcode == Opcodes.GETSTATIC -> {
                        val fieldInsnNode = cur as FieldInsnNode?
                        val className = fieldInsnNode!!.owner
                        if (isAnonymousSingletonLoad(className, fieldInsnNode.name)) {
                            recordTransformation(
                                    AnonymousObjectTransformationInfo(
                                            className, awaitClassReification, isAlreadyRegenerated(className), true,
                                            inliningContext.nameGenerator
                                    )
                            )
                            awaitClassReification = false
                        }
                        else if (isWhenMappingAccess(className, fieldInsnNode.name)) {
                            recordTransformation(
                                    WhenMappingTransformationInfo(
                                            className, inliningContext.nameGenerator, isAlreadyRegenerated(className), fieldInsnNode
                                    )
                            )
                        }
                    }

                    cur.opcode == Opcodes.POP -> getLambdaIfExistsAndMarkInstructions(frame.top()!!, true, instructions, sources, toDelete)?.let {
                        toDelete.add(cur)
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
                            nodeRemapper.originalLambdaInternalName == fieldInsn.owner) {
                            val stackTransformations = mutableSetOf<AbstractInsnNode>()
                            val lambdaInfo = getLambdaIfExistsAndMarkInstructions(frame.peek(1)!!, false, instructions, sources, stackTransformations)
                            if (lambdaInfo != null && stackTransformations.all { it is VarInsnNode }) {
                                assert(lambdaInfo.lambdaClassType.internalName == nodeRemapper.originalLambdaInternalName) {
                                    "Wrong bytecode template for contract template: ${lambdaInfo.lambdaClassType.internalName} != ${nodeRemapper.originalLambdaInternalName}"
                                }
                                fieldInsn.name = FieldRemapper.foldName(fieldInsn.name)
                                fieldInsn.opcode = Opcodes.PUTSTATIC
                                toDelete.addAll(stackTransformations)
                            }
                        }
                    }
                }
            }
            else {
                //given frame is <tt>null</tt> if and only if the corresponding instruction cannot be reached (dead code).
                //clean dead code otherwise there is problems in unreachable finally block, don't touch label it cause try/catch/finally problems
                if (cur.type == AbstractInsnNode.LABEL) {
                    //NB: Cause we generate exception table for default handler using gaps (see ExpressionCodegen.visitTryExpression)
                    //it may occurs that interval for default handler starts before catch start label, so this label seems as dead,
                    //but as result all this labels will be merged into one (see KT-5863)
                }
                else {
                    toDelete.add(cur)
                }
            }
        }

        processingNode.remove(toDelete)

        //clean dead try/catch blocks
        processingNode.tryCatchBlocks.removeIf { it.isMeaningless() }

        return processingNode
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
        val lambdaInfo = inliningContext.lambdaInfo ?: return
        if (!lambdaInfo.invokeMethodDescriptor.isSuspend) return
        val sources = analyzeMethodNodeBeforeInline(processingNode)
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
            paramTypes.reversed().asSequence().withIndex()
                .filter { it.value == languageVersionSettings.continuationAsmType() || it.value == OBJECT_TYPE }
                .flatMap { frame.getStack(frame.stackSize - it.index - 1).insns.asSequence() }
                .filter { it in aload0s }.let { toReplace.addAll(it) }
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

    private fun isSuspendCall(invoke: AbstractInsnNode?): Boolean {
        if (invoke !is MethodInsnNode) return false
        // We can't have suspending constructors.
        assert(invoke.opcode != Opcodes.INVOKESPECIAL)
        if (Type.getReturnType(invoke.desc) != OBJECT_TYPE) return false
        return Type.getArgumentTypes(invoke.desc).let { it.isNotEmpty() && it.last() == languageVersionSettings.continuationAsmType() }
    }

    private fun preprocessNodeBeforeInline(node: MethodNode, labelOwner: LabelOwner) {
        try {
            FixStackWithLabelNormalizationMethodTransformer().transform("fake", node)
        }
        catch (e: Throwable) {
            throw wrapException(e, node, "couldn't inline method call")
        }

        if (shouldPreprocessApiVersionCalls) {
            val targetApiVersion = inliningContext.state.languageVersionSettings.apiVersion
            ApiVersionCallsPreprocessingMethodTransformer(targetApiVersion).transform("fake", node)
        }

        val frames = analyzeMethodNodeBeforeInline(node)

        val localReturnsNormalizer = LocalReturnsNormalizer()

        for ((index, insnNode) in node.instructions.toArray().withIndex()) {
            val frame = frames[index] ?: continue
            // Don't care about dead code, it will be eliminated

            if (!isReturnOpcode(insnNode.opcode)) continue

            var insertBeforeInsn = insnNode

            // TODO extract isLocalReturn / isNonLocalReturn, see processReturns
            val labelName = getMarkedReturnLabelOrNull(insnNode)
            if (labelName != null) {
                if (!labelOwner.isMyLabel(labelName)) continue
                insertBeforeInsn = insnNode.previous
            }

            localReturnsNormalizer.addLocalReturnToTransform(insnNode, insertBeforeInsn, frame)
        }

        localReturnsNormalizer.transform(node)
    }

    private fun isAnonymousClassThatMustBeRegenerated(type: Type?): Boolean {
        if (type == null || type.sort != Type.OBJECT) return false
        val info = inliningContext.findAnonymousObjectTransformationInfo(type.internalName)
        return info != null && info.shouldRegenerate(true)
    }

    private fun buildConstructorInvocation(
            anonymousType: String,
            desc: String,
            lambdaMapping: Map<Int, LambdaInfo>,
            needReification: Boolean,
            capturesAnonymousObjectThatMustBeRegenerated: Boolean
    ): AnonymousObjectTransformationInfo {

        val info = AnonymousObjectTransformationInfo(
                anonymousType, needReification, lambdaMapping,
                inliningContext.classRegeneration,
                isAlreadyRegenerated(anonymousType),
                desc,
                false,
                inliningContext.nameGenerator,
                capturesAnonymousObjectThatMustBeRegenerated
        )

        val memoizeAnonymousObject = inliningContext.findAnonymousObjectTransformationInfo(anonymousType)
        if (memoizeAnonymousObject == null ||
            //anonymous object could be inlined in several context without transformation (keeps same class name)
            // and on further inlining such code some of such cases would be transformed and some not,
            // so we should distinguish one classes from another more clearly
            !memoizeAnonymousObject.shouldRegenerate(isSameModule) &&
            info.shouldRegenerate(isSameModule)
        ) {

            inliningContext.recordIfNotPresent(anonymousType, info)
        }
        return info
    }

    private fun isAlreadyRegenerated(owner: String): Boolean {
        return inliningContext.typeRemapper.hasNoAdditionalMapping(owner)
    }

    internal fun getLambdaIfExists(insnNode: AbstractInsnNode): LambdaInfo? {
        return when {
            insnNode.opcode == Opcodes.ALOAD ->
                getLambdaIfExists((insnNode as VarInsnNode).`var`)
            insnNode is FieldInsnNode && insnNode.name.startsWith(CAPTURED_FIELD_FOLD_PREFIX) ->
                findCapturedField(insnNode, nodeRemapper).lambda
            else ->
                null
        }
    }

    private fun getLambdaIfExists(varIndex: Int): LambdaInfo? {
        if (varIndex < parameters.argsSizeOnStack) {
            return parameters.getParameterByDeclarationSlot(varIndex).lambda
        }
        return null
    }

    private fun transformCaptured(node: MethodNode) {
        if (nodeRemapper.isRoot) {
            return
        }

        if (inliningContext.isInliningLambda && inliningContext.lambdaInfo is IrExpressionLambda) {
            val capturedVars = inliningContext.lambdaInfo.capturedVars
            var offset = parameters.realParametersSizeOnStack
            val map = capturedVars.map {
                offset to it.also { offset += it.type.size }
            }.toMap()

            var cur: AbstractInsnNode? = node.instructions.first
            while (cur != null) {
                if (cur is VarInsnNode && cur.opcode == Opcodes.ALOAD && map.contains(cur.`var`)) {
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

    private fun wrapException(originalException: Throwable, node: MethodNode, errorSuffix: String): RuntimeException {
        return if (originalException is InlineException) {
            InlineException("$errorPrefix: $errorSuffix", originalException)
        }
        else {
            InlineException("$errorPrefix: $errorSuffix\nCause: ${node.nodeText}", originalException)
        }
    }

    private class LocalReturnsNormalizer {
        private class LocalReturn(
                private val returnInsn: AbstractInsnNode,
                private val insertBeforeInsn: AbstractInsnNode,
                private val frame: Frame<SourceValue>
        ) {

            fun transform(insnList: InsnList, returnVariableIndex: Int) {
                val isReturnWithValue = returnInsn.opcode != Opcodes.RETURN

                val expectedStackSize = if (isReturnWithValue) 1 else 0
                val actualStackSize = frame.stackSize
                if (expectedStackSize == actualStackSize) return

                var stackSize = actualStackSize
                if (isReturnWithValue) {
                    val storeOpcode = Opcodes.ISTORE + returnInsn.opcode - Opcodes.IRETURN
                    insnList.insertBefore(insertBeforeInsn, VarInsnNode(storeOpcode, returnVariableIndex))
                    stackSize--
                }

                while (stackSize > 0) {
                    val stackElementSize = frame.getStack(stackSize - 1).getSize()
                    val popOpcode = if (stackElementSize == 1) Opcodes.POP else Opcodes.POP2
                    insnList.insertBefore(insertBeforeInsn, InsnNode(popOpcode))
                    stackSize--
                }

                if (isReturnWithValue) {
                    val loadOpcode = Opcodes.ILOAD + returnInsn.opcode - Opcodes.IRETURN
                    insnList.insertBefore(insertBeforeInsn, VarInsnNode(loadOpcode, returnVariableIndex))
                }
            }
        }

        private val localReturns = SmartList<LocalReturn>()

        private var returnVariableSize = 0
        private var returnOpcode = -1

        internal fun addLocalReturnToTransform(
                returnInsn: AbstractInsnNode,
                insertBeforeInsn: AbstractInsnNode,
                sourceValueFrame: Frame<SourceValue>
        ) {
            assert(isReturnOpcode(returnInsn.opcode)) { "return instruction expected" }
            assert(returnOpcode < 0 || returnOpcode == returnInsn.opcode) { "Return op should be " + Printer.OPCODES[returnOpcode] + ", got " + Printer.OPCODES[returnInsn.opcode] }
            returnOpcode = returnInsn.opcode

            localReturns.add(LocalReturn(returnInsn, insertBeforeInsn, sourceValueFrame))

            if (returnInsn.opcode != Opcodes.RETURN) {
                returnVariableSize = if (returnInsn.opcode == Opcodes.LRETURN || returnInsn.opcode == Opcodes.DRETURN) {
                    2
                }
                else {
                    1
                }
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
            @JvmField val finallyIntervalEnd: LabelNode
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

        private fun analyzeMethodNodeBeforeInline(node: MethodNode): Array<Frame<SourceValue>?> {
            val analyzer = object : Analyzer<SourceValue>(SourceInterpreter()) {
                override fun newFrame(nLocals: Int, nStack: Int): Frame<SourceValue> {

                    return object : Frame<SourceValue>(nLocals, nStack) {
                        @Throws(AnalyzerException::class)
                        override fun execute(insn: AbstractInsnNode, interpreter: Interpreter<SourceValue>) {
                            // This can be a void non-local return from a non-void method; Frame#execute would throw and do nothing else.
                            if (insn.opcode == Opcodes.RETURN) return
                            super.execute(insn, interpreter)
                        }
                    }
                }
            }

            try {
                return analyzer.analyze("fake", node)
            }
            catch (e: AnalyzerException) {
                throw RuntimeException(e)
            }

        }

        //remove next template:
        //      aload x
        //      LDC paramName
        //      INTRINSICS_CLASS_NAME.checkParameterIsNotNull(...)
        private fun removeClosureAssertions(node: MethodNode) {
            val toDelete = arrayListOf<AbstractInsnNode>()
            InsnSequence(node.instructions).filterIsInstance<MethodInsnNode>().forEach {
                methodInsnNode ->
                if (methodInsnNode.name == "checkParameterIsNotNull" && methodInsnNode.owner == IntrinsicMethods.INTRINSICS_CLASS_NAME) {
                    val prev = methodInsnNode.previous
                    assert(Opcodes.LDC == prev?.opcode) { "'checkParameterIsNotNull' should go after LDC but $prev" }
                    val prevPev = methodInsnNode.previous.previous
                    assert(Opcodes.ALOAD == prevPev?.opcode) { "'checkParameterIsNotNull' should be invoked on local var, but $prev" }

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
                addAll(InsnSequence(aload0.next, null).filter { it.isMeaningful }.takeWhile {
                    insnNode ->
                    insnNode is FieldInsnNode && "this$0" == insnNode.name
                }.toList())
            }

            return lambdaAccessChain.apply {
                last().getNextMeaningful().takeIf { insn -> insn is FieldInsnNode }?.also {
                    //captured field access
                    insn -> add(insn)
                }
            }
        }

        private fun putStackValuesIntoLocalsForLambdaOnInvoke(
            directOrder: List<Type>,
            directOrderOfArguments: List<ParameterDescriptor>,
            directOrderOfInvokeParameters: List<ValueParameterDescriptor>,
            shift: Int,
            iv: InstructionAdapter,
            descriptor: String
        ) {
            val actualParams = Type.getArgumentTypes(descriptor)
            assert(actualParams.size == directOrder.size) {
                "Number of expected and actual params should be equals, but ${actualParams.size} != ${directOrder.size}}!"
            }

            var currentShift = shift + directOrder.sumBy { it.size }

            val safeToUseArgumentKotlinType =
                directOrder.size == directOrderOfArguments.size && directOrderOfArguments.size == directOrderOfInvokeParameters.size

            for (index in directOrder.lastIndex downTo 0) {
                val type = directOrder[index]
                currentShift -= type.size
                val typeOnStack = actualParams[index]

                val argumentKotlinType: KotlinType?
                val invokeParameterKotlinType: KotlinType?
                if (safeToUseArgumentKotlinType) {
                    argumentKotlinType = directOrderOfArguments[index].type
                    invokeParameterKotlinType = directOrderOfInvokeParameters[index].type
                } else {
                    argumentKotlinType = null
                    invokeParameterKotlinType = null
                }

                if (typeOnStack != type || invokeParameterKotlinType != argumentKotlinType) {
                    StackValue.onStack(typeOnStack, invokeParameterKotlinType).put(type, argumentKotlinType, iv)
                }
                iv.store(currentShift, type)
            }
        }

        //process local and global returns (local substituted with goto end-label global kept unchanged)
        @JvmStatic
        fun processReturns(
                node: MethodNode, labelOwner: LabelOwner, remapReturn: Boolean, endLabel: Label?
        ): List<PointForExternalFinallyBlocks> {
            if (!remapReturn) {
                return emptyList()
            }
            val result = ArrayList<PointForExternalFinallyBlocks>()
            val instructions = node.instructions
            var insnNode: AbstractInsnNode? = instructions.first
            while (insnNode != null) {
                if (isReturnOpcode(insnNode.opcode)) {
                    var isLocalReturn = true
                    val labelName = getMarkedReturnLabelOrNull(insnNode)

                    if (labelName != null) {
                        isLocalReturn = labelOwner.isMyLabel(labelName)
                        //remove global return flag
                        if (isLocalReturn) {
                            instructions.remove(insnNode.previous)
                        }
                    }

                    if (isLocalReturn && endLabel != null) {
                        val nop = InsnNode(Opcodes.NOP)
                        instructions.insert(insnNode, nop)

                        val labelNode = endLabel.info as LabelNode
                        val jumpInsnNode = JumpInsnNode(Opcodes.GOTO, labelNode)
                        instructions.insert(nop, jumpInsnNode)

                        instructions.remove(insnNode)
                        insnNode = jumpInsnNode
                    }

                    //generate finally block before nonLocalReturn flag/return/goto
                    val label = LabelNode()
                    instructions.insert(insnNode, label)
                    result.add(PointForExternalFinallyBlocks(
                            getInstructionToInsertFinallyBefore(insnNode, isLocalReturn), getReturnType(insnNode.opcode), label
                    ))
                }
                insnNode = insnNode.next
            }
            return result
        }

        private fun getInstructionToInsertFinallyBefore(nonLocalReturnOrJump: AbstractInsnNode, isLocal: Boolean): AbstractInsnNode {
            return if (isLocal) nonLocalReturnOrJump else nonLocalReturnOrJump.previous
        }
    }
}

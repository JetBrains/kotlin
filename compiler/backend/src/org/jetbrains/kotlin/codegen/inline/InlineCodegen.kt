/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.AsmUtil.isPrimitive
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.*
import kotlin.math.max

abstract class InlineCodegen<out T : BaseExpressionCodegen>(
    protected val codegen: T,
    protected val state: GenerationState,
    protected val jvmSignature: JvmMethodSignature,
    private val typeParameterMappings: TypeParameterMappings<*>,
    protected val sourceCompiler: SourceCompilerForInline,
    private val reifiedTypeInliner: ReifiedTypeInliner<*>
) {
    private val initialFrameSize = codegen.frameMap.currentSize

    protected val invocationParamBuilder = ParametersBuilder.newBuilder()
    private val maskValues = ArrayList<Int>()
    private var maskStartIndex = -1
    private var methodHandleInDefaultMethodIndex = -1

    protected fun generateStub(text: String, codegen: BaseExpressionCodegen) {
        leaveTemps()
        AsmUtil.genThrow(codegen.visitor, "java/lang/UnsupportedOperationException", "Call is part of inline cycle: $text")
    }

    fun compileInline(): SMAPAndMethodNode {
        return sourceCompiler.compileInlineFunction(jvmSignature).apply {
            node.preprocessSuspendMarkers(forInline = true, keepFakeContinuation = false)
        }
    }

    fun performInline(registerLineNumberAfterwards: Boolean, isInlineOnly: Boolean) {
        var nodeAndSmap: SMAPAndMethodNode? = null
        try {
            nodeAndSmap = compileInline()
            val result = inlineCall(nodeAndSmap, isInlineOnly)
            leaveTemps()
            codegen.propagateChildReifiedTypeParametersUsages(result.reifiedTypeParametersUsages)
            codegen.markLineNumberAfterInlineIfNeeded(registerLineNumberAfterwards)
            state.factory.removeClasses(result.calcClassesToRemove())
        } catch (e: CompilationException) {
            throw e
        } catch (e: InlineException) {
            throw CompilationException(
                "Couldn't inline method call: ${sourceCompiler.callElementText}",
                e, sourceCompiler.callElement as? PsiElement
            )
        } catch (e: Exception) {
            throw CompilationException(
                "Couldn't inline method call: ${sourceCompiler.callElementText}\nMethod: ${nodeAndSmap?.node?.nodeText}",
                e, sourceCompiler.callElement as? PsiElement
            )
        }
    }

    private fun inlineCall(nodeAndSmap: SMAPAndMethodNode, isInlineOnly: Boolean): InlineResult {
        val node = nodeAndSmap.node
        if (maskStartIndex != -1) {
            val parameters = invocationParamBuilder.buildParameters()
            val infos = expandMaskConditionsAndUpdateVariableNodes(
                node, maskStartIndex, maskValues, methodHandleInDefaultMethodIndex,
                parameters.parameters.filter { it.functionalArgument === DefaultValueOfInlineParameter }
                    .mapTo<_, _, MutableCollection<Int>>(mutableSetOf()) { parameters.getDeclarationSlot(it) }
            )
            for (info in infos) {
                val lambda = DefaultLambda(info, sourceCompiler, node.name.substringBeforeLast("\$default"))
                parameters.getParameterByDeclarationSlot(info.offset).functionalArgument = lambda
                if (info.needReification) {
                    lambda.reifiedTypeParametersUsages.mergeAll(reifiedTypeInliner.reifyInstructions(lambda.node.node))
                }
                for (captured in lambda.capturedVars) {
                    val param = invocationParamBuilder.addCapturedParam(captured, captured.fieldName, false)
                    param.remapValue = StackValue.local(codegen.frameMap.enterTemp(param.type), param.type)
                    param.isSynthetic = true
                }
            }
        }

        val reificationResult = reifiedTypeInliner.reifyInstructions(node)

        val parameters = invocationParamBuilder.buildParameters()

        val info = RootInliningContext(
            state, codegen.inlineNameGenerator.subGenerator(jvmSignature.asmMethod.name),
            sourceCompiler, sourceCompiler.inlineCallSiteInfo, reifiedTypeInliner, typeParameterMappings
        )

        val sourceMapper = sourceCompiler.sourceMapper
        val sourceInfo = sourceMapper.sourceInfo!!
        val callSite = SourcePosition(codegen.lastLineNumber, sourceInfo.sourceFileName!!, sourceInfo.pathOrCleanFQN)
        val inliner = MethodInliner(
            node, parameters, info, FieldRemapper(null, null, parameters), sourceCompiler.isCallInsideSameModuleAsCallee,
            "Method inlining " + sourceCompiler.callElementText,
            SourceMapCopier(sourceMapper, nodeAndSmap.classSMAP, callSite),
            info.callSiteInfo,
            isInlineOnlyMethod = isInlineOnly,
            !isInlinedToInlineFunInKotlinRuntime(),
            maskStartIndex,
            maskStartIndex + maskValues.size,
        ) //with captured

        val remapper = LocalVarRemapper(parameters, initialFrameSize)

        val adapter = createEmptyMethodNode()
        //hack to keep linenumber info, otherwise jdi will skip begin of linenumber chain
        adapter.visitInsn(Opcodes.NOP)

        val result = inliner.doInline(adapter, remapper, true, mapOf())
        result.reifiedTypeParametersUsages.mergeAll(reificationResult)

        val infos = MethodInliner.processReturns(adapter, sourceCompiler.getContextLabels(), null)
        generateAndInsertFinallyBlocks(
            adapter, infos, (remapper.remap(parameters.argsSizeOnStack).value as StackValue.Local).index
        )
        if (!sourceCompiler.isFinallyMarkerRequired) {
            removeFinallyMarkers(adapter)
        }

        // In case `codegen.visitor` is `<clinit>`, initializer for the `$assertionsDisabled` field
        // needs to be inserted before the code that actually uses it.
        if (info.generateAssertField) {
            generateAssertField()
        }

        val shouldSpillStack = node.requiresEmptyStackOnEntry()
        if (shouldSpillStack) {
            addInlineMarker(codegen.visitor, true)
        }
        adapter.accept(MethodBodyVisitor(codegen.visitor))
        if (shouldSpillStack) {
            addInlineMarker(codegen.visitor, false)
        }
        return result
    }

    private fun generateAndInsertFinallyBlocks(
        intoNode: MethodNode,
        insertPoints: List<MethodInliner.PointForExternalFinallyBlocks>,
        offsetForFinallyLocalVar: Int
    ) {
        if (!sourceCompiler.hasFinallyBlocks()) return

        val extensionPoints = insertPoints.associateBy { it.beforeIns }
        val processor = DefaultProcessor(intoNode, offsetForFinallyLocalVar)

        var curFinallyDepth = 0
        var curInstr: AbstractInsnNode? = intoNode.instructions.first
        while (curInstr != null) {
            processor.processInstruction(curInstr, true)
            if (isFinallyStart(curInstr)) {
                //TODO depth index calc could be more precise
                curFinallyDepth = getConstant(curInstr.previous)
            }

            val extension = extensionPoints[curInstr]
            if (extension != null) {
                var nextFreeLocalIndex = processor.nextFreeLocalIndex
                for (local in processor.localVarsMetaInfo.currentIntervals) {
                    val size = Type.getType(local.node.desc).size
                    nextFreeLocalIndex = max(offsetForFinallyLocalVar + local.node.index + size, nextFreeLocalIndex)
                }

                val start = Label()
                val finallyNode = createEmptyMethodNode()
                finallyNode.visitLabel(start)
                val mark = codegen.frameMap.skipTo(nextFreeLocalIndex)
                sourceCompiler.generateFinallyBlocks(
                    finallyNode, curFinallyDepth, extension.returnType, extension.finallyIntervalEnd.label, extension.jumpTarget
                )
                mark.dropTo()
                insertNodeBefore(finallyNode, intoNode, curInstr)

                val splitBy = SimpleInterval(start.info as LabelNode, extension.finallyIntervalEnd)
                processor.tryBlocksMetaInfo.splitAndRemoveCurrentIntervals(splitBy, true)
                processor.localVarsMetaInfo.splitAndRemoveCurrentIntervals(splitBy, true)
                finallyNode.localVariables.forEach {
                    processor.localVarsMetaInfo.addNewInterval(LocalVarNodeWrapper(it))
                }
            }

            curInstr = curInstr.next
        }

        processor.substituteTryBlockNodes(intoNode)
        processor.substituteLocalVarTable(intoNode)
    }

    protected abstract fun generateAssertField()

    protected abstract fun isInlinedToInlineFunInKotlinRuntime(): Boolean

    protected fun rememberClosure(parameterType: Type, index: Int, lambdaInfo: LambdaInfo) {
        invocationParamBuilder.addNextValueParameter(parameterType, true, null, index).functionalArgument = lambdaInfo
    }

    protected fun putCapturedToLocalVal(stackValue: StackValue, capturedParam: CapturedParamDesc, kotlinType: KotlinType?) {
        val info = invocationParamBuilder.addCapturedParam(capturedParam, capturedParam.fieldName, false)
        if (stackValue.isLocalWithNoBoxing(JvmKotlinType(info.type, kotlinType))) {
            info.remapValue = stackValue
        } else {
            stackValue.put(info.type, kotlinType, codegen.visitor)
            val local = StackValue.local(codegen.frameMap.enterTemp(info.type), info.type)
            local.store(StackValue.onStack(info.type), codegen.visitor)
            info.remapValue = local
            info.isSynthetic = true
        }
    }

    protected fun putArgumentToLocalVal(jvmKotlinType: JvmKotlinType, stackValue: StackValue, parameterIndex: Int, kind: ValueKind) {
        if (kind === ValueKind.DEFAULT_MASK || kind === ValueKind.METHOD_HANDLE_IN_DEFAULT) {
            return processDefaultMaskOrMethodHandler(stackValue, kind)
        }

        val info = when (parameterIndex) {
            -1 -> invocationParamBuilder.addNextParameter(jvmKotlinType.type, false)
            else -> invocationParamBuilder.addNextValueParameter(jvmKotlinType.type, false, null, parameterIndex)
        }
        info.functionalArgument = when (kind) {
            ValueKind.READ_OF_INLINE_LAMBDA_FOR_INLINE_SUSPEND_PARAMETER ->
                NonInlineArgumentForInlineSuspendParameter.INLINE_LAMBDA_AS_VARIABLE
            ValueKind.READ_OF_OBJECT_FOR_INLINE_SUSPEND_PARAMETER ->
                NonInlineArgumentForInlineSuspendParameter.OTHER
            ValueKind.DEFAULT_INLINE_PARAMETER ->
                DefaultValueOfInlineParameter
            else -> null
        }
        when {
            kind === ValueKind.DEFAULT_PARAMETER || kind === ValueKind.DEFAULT_INLINE_PARAMETER ->
                codegen.frameMap.enterTemp(info.type) // the inline function will put the value into this slot
            stackValue.isLocalWithNoBoxing(jvmKotlinType) ->
                info.remapValue = stackValue
            else -> {
                stackValue.put(info.type, jvmKotlinType.kotlinType, codegen.visitor)
                codegen.visitor.store(codegen.frameMap.enterTemp(info.type), info.type)
            }
        }
    }

    private fun leaveTemps() {
        invocationParamBuilder.listAllParams().asReversed().forEach { param ->
            if (!param.isSkippedOrRemapped || CapturedParamInfo.isSynthetic(param)) {
                codegen.frameMap.leaveTemp(param.type)
            }
        }
    }

    private fun processDefaultMaskOrMethodHandler(value: StackValue, kind: ValueKind) {
        assert(value is StackValue.Constant) { "Additional default method argument should be constant, but $value" }
        val constantValue = (value as StackValue.Constant).value
        if (kind === ValueKind.DEFAULT_MASK) {
            assert(constantValue is Int) { "Mask should be of Integer type, but $constantValue" }
            maskValues.add(constantValue as Int)
            if (maskStartIndex == -1) {
                maskStartIndex = invocationParamBuilder.listAllParams().sumOf {
                    if (it is CapturedParamInfo) 0 else it.type.size
                }
            }
        } else {
            assert(constantValue == null) { "Additional method handle for default argument should be null, but " + constantValue!! }
            methodHandleInDefaultMethodIndex = maskStartIndex + maskValues.size
        }
    }

    companion object {
        private fun StackValue.isLocalWithNoBoxing(expected: JvmKotlinType): Boolean =
            isPrimitive(expected.type) == isPrimitive(type) &&
                    !StackValue.requiresInlineClassBoxingOrUnboxing(type, kotlinType, expected.type, expected.kotlinType) &&
                    (this is StackValue.Local || isCapturedInlineParameter())

        private fun StackValue.isCapturedInlineParameter(): Boolean {
            val field = if (this is StackValue.FieldForSharedVar) receiver else this
            return field is StackValue.Field && field.descriptor is ParameterDescriptor &&
                    InlineUtil.isInlineParameter(field.descriptor) &&
                    InlineUtil.isInline(field.descriptor.containingDeclaration)
        }

        // Stack spilling before inline function call is required if the inlined bytecode has:
        //   1. try-catch blocks - otherwise the stack spilling before and after them will not be correct;
        //   2. suspension points - again, the stack spilling around them is otherwise wrong;
        //   3. loops - OpenJDK cannot JIT-optimize between loop iterations if the stack is not empty.
        // Instead of checking for loops precisely, we just check if there are any backward jumps -
        // that is, a jump from instruction #i to instruction #j where j < i.
        private fun MethodNode.requiresEmptyStackOnEntry(): Boolean = tryCatchBlocks.isNotEmpty() ||
                instructions.toArray().any { isBeforeSuspendMarker(it) || isBeforeInlineSuspendMarker(it) || isBackwardsJump(it) }

        private fun MethodNode.isBackwardsJump(insn: AbstractInsnNode): Boolean = when (insn) {
            is JumpInsnNode -> isBackwardsJump(insn, insn.label)
            is LookupSwitchInsnNode ->
                insn.dflt?.let { to -> isBackwardsJump(insn, to) } == true || insn.labels.any { to -> isBackwardsJump(insn, to) }
            is TableSwitchInsnNode ->
                insn.dflt?.let { to -> isBackwardsJump(insn, to) } == true || insn.labels.any { to -> isBackwardsJump(insn, to) }
            else -> false
        }

        private fun MethodNode.isBackwardsJump(from: AbstractInsnNode, to: LabelNode): Boolean =
            instructions.indexOf(to) < instructions.indexOf(from)
    }
}

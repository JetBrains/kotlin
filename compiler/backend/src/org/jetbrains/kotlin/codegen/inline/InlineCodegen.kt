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
    protected val expressionMap = linkedMapOf<Int, FunctionalArgument>()
    protected val maskValues = ArrayList<Int>()
    protected var maskStartIndex = -1
    protected var methodHandleInDefaultMethodIndex = -1

    protected fun generateStub(text: String, codegen: BaseExpressionCodegen) {
        leaveTemps()
        AsmUtil.genThrow(codegen.visitor, "java/lang/UnsupportedOperationException", "Call is part of inline cycle: $text")
    }

    fun performInline(registerLineNumberAfterwards: Boolean, isInlineOnly: Boolean, isSuspend: Boolean) {
        var nodeAndSmap: SMAPAndMethodNode? = null
        try {
            nodeAndSmap = sourceCompiler.compileInlineFunction(jvmSignature).apply {
                node.preprocessSuspendMarkers(forInline = true, keepFakeContinuation = false)
            }
            val result = inlineCall(nodeAndSmap, isInlineOnly, isSuspend)
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

    private fun canSkipStackSpillingOnInline(methodNode: MethodNode): Boolean {
        // Stack spilling before inline function 'f' call is required if:
        //  - 'f' is a suspend function
        //  - 'f' has try-catch blocks
        //  - 'f' has loops
        //
        // Instead of checking for loops precisely, we just check if there are any backward jumps -
        // that is, a jump from instruction #i to instruction #j where j < i
        if (methodNode.tryCatchBlocks.isNotEmpty()) return false

        fun isBackwardJump(fromIndex: Int, toLabel: LabelNode) =
            methodNode.instructions.indexOf(toLabel) < fromIndex

        val insns = methodNode.instructions.toArray()
        for (i in insns.indices) {
            when (val insn = insns[i]) {
                is JumpInsnNode ->
                    if (isBackwardJump(i, insn.label)) return false

                is LookupSwitchInsnNode -> {
                    insn.dflt?.let {
                        if (isBackwardJump(i, it)) return false
                    }
                    if (insn.labels.any { isBackwardJump(i, it) }) return false
                }

                is TableSwitchInsnNode -> {
                    insn.dflt?.let {
                        if (isBackwardJump(i, it)) return false
                    }
                    if (insn.labels.any { isBackwardJump(i, it) }) return false
                }
            }
        }

        return true
    }

    private fun inlineCall(nodeAndSmap: SMAPAndMethodNode, isInlineOnly: Boolean, isSuspend: Boolean): InlineResult {
        val node = nodeAndSmap.node
        if (maskStartIndex != -1) {
            for (lambda in extractDefaultLambdas(node)) {
                invocationParamBuilder.buildParameters().getParameterByDeclarationSlot(lambda.offset).functionalArgument = lambda
                val prev = expressionMap.put(lambda.offset, lambda)
                assert(prev == null) { "Lambda with offset ${lambda.offset} already exists: $prev" }
                if (lambda.needReification) {
                    lambda.reifiedTypeParametersUsages.mergeAll(reifiedTypeInliner.reifyInstructions(lambda.node.node))
                }
                rememberCapturedForDefaultLambda(lambda)
            }
        }

        val reificationResult = reifiedTypeInliner.reifyInstructions(node)

        val parameters = invocationParamBuilder.buildParameters()

        val info = RootInliningContext(
            expressionMap, state, codegen.inlineNameGenerator.subGenerator(jvmSignature.asmMethod.name),
            sourceCompiler, sourceCompiler.inlineCallSiteInfo, reifiedTypeInliner, typeParameterMappings
        )

        val sourceMapper = sourceCompiler.sourceMapper
        val sourceInfo = sourceMapper.sourceInfo!!
        val callSite = SourcePosition(codegen.lastLineNumber, sourceInfo.sourceFileName!!, sourceInfo.pathOrCleanFQN)
        val inliner = MethodInliner(
            node, parameters, info, FieldRemapper(null, null, parameters), sourceCompiler.isCallInsideSameModuleAsCallee,
            "Method inlining " + sourceCompiler.callElementText,
            SourceMapCopier(sourceMapper, nodeAndSmap.classSMAP, callSite),
            info.callSiteInfo, if (isInlineOnly) InlineOnlySmapSkipper(codegen) else null,
            !isInlinedToInlineFunInKotlinRuntime()
        ) //with captured

        val remapper = LocalVarRemapper(parameters, initialFrameSize)

        val adapter = createEmptyMethodNode()
        //hack to keep linenumber info, otherwise jdi will skip begin of linenumber chain
        adapter.visitInsn(Opcodes.NOP)

        val result = inliner.doInline(adapter, remapper, true, mapOf())
        result.reifiedTypeParametersUsages.mergeAll(reificationResult)

        val infos = MethodInliner.processReturns(adapter, sourceCompiler.getContextLabels(), null)
        generateAndInsertFinallyBlocks(
            adapter, infos, (remapper.remap(parameters.argsSizeOnStack + 1).value as StackValue.Local).index
        )
        if (!sourceCompiler.isFinallyMarkerRequired) {
            removeFinallyMarkers(adapter)
        }

        // In case `codegen.visitor` is `<clinit>`, initializer for the `$assertionsDisabled` field
        // needs to be inserted before the code that actually uses it.
        generateAssertFieldIfNeeded(info)

        val shouldSpillStack = isSuspend || !canSkipStackSpillingOnInline(node)
        if (shouldSpillStack) {
            addInlineMarker(codegen.visitor, true)
        }
        adapter.accept(MethodBodyVisitor(codegen.visitor))
        if (shouldSpillStack) {
            addInlineMarker(codegen.visitor, false)
        }
        return result
    }

    abstract fun extractDefaultLambdas(node: MethodNode): List<DefaultLambda>

    protected inline fun <T> extractDefaultLambdas(
        node: MethodNode, parameters: Map<Int, T>, block: ExtractedDefaultLambda.(T) -> DefaultLambda
    ): List<DefaultLambda> = expandMaskConditionsAndUpdateVariableNodes(
        node, maskStartIndex, maskValues, methodHandleInDefaultMethodIndex, parameters.keys
    ).map {
        it.block(parameters[it.offset]!!)
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
                val start = Label()

                val finallyNode = createEmptyMethodNode()
                finallyNode.visitLabel(start)

                val finallyCodegen =
                    sourceCompiler.createCodegenForExternalFinallyBlockGenerationOnNonLocalReturn(finallyNode, curFinallyDepth)

                val frameMap = finallyCodegen.frameMap
                val mark = frameMap.mark()
                var marker = -1
                val intervals = processor.localVarsMetaInfo.currentIntervals
                for (interval in intervals) {
                    marker = max(interval.node.index + 1, marker)
                }
                while (frameMap.currentSize < max(processor.nextFreeLocalIndex, offsetForFinallyLocalVar + marker)) {
                    frameMap.enterTemp(Type.INT_TYPE)
                }

                sourceCompiler.generateFinallyBlocksIfNeeded(
                    finallyCodegen, extension.returnType, extension.finallyIntervalEnd.label, extension.jumpTarget
                )

                //Exception table for external try/catch/finally blocks will be generated in original codegen after exiting this method
                insertNodeBefore(finallyNode, intoNode, curInstr)

                val splitBy = SimpleInterval(start.info as LabelNode, extension.finallyIntervalEnd)
                processor.tryBlocksMetaInfo.splitAndRemoveCurrentIntervals(splitBy, true)
                processor.localVarsMetaInfo.splitAndRemoveCurrentIntervals(splitBy, true)

                mark.dropTo()
            }

            curInstr = curInstr.next
        }

        processor.substituteTryBlockNodes(intoNode)
        processor.substituteLocalVarTable(intoNode)
    }

    protected abstract fun generateAssertFieldIfNeeded(info: RootInliningContext)

    private fun isInlinedToInlineFunInKotlinRuntime(): Boolean {
        val codegen = this.codegen as? ExpressionCodegen ?: return false
        val caller = codegen.context.functionDescriptor
        if (!caller.isInline) return false
        val callerPackage = DescriptorUtils.getParentOfType(caller, PackageFragmentDescriptor::class.java) ?: return false
        return callerPackage.fqName.asString().let {
            // package either equals to 'kotlin' or starts with 'kotlin.'
            it.startsWith("kotlin") && (it.length <= 6 || it[6] == '.')
        }
    }

    protected fun rememberClosure(parameterType: Type, index: Int, lambdaInfo: LambdaInfo) {
        val closureInfo = invocationParamBuilder.addNextValueParameter(parameterType, true, null, index)
        closureInfo.functionalArgument = lambdaInfo
        expressionMap[closureInfo.index] = lambdaInfo
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

        val info = invocationParamBuilder.addNextValueParameter(jvmKotlinType.type, false, null, parameterIndex)
        info.functionalArgument = when (kind) {
            ValueKind.NON_INLINEABLE_ARGUMENT_FOR_INLINE_PARAMETER_CALLED_IN_SUSPEND ->
                NonInlineableArgumentForInlineableParameterCalledInSuspend
            ValueKind.NON_INLINEABLE_ARGUMENT_FOR_INLINE_SUSPEND_PARAMETER ->
                NonInlineableArgumentForInlineableSuspendParameter
            else -> null
        }
        when {
            kind === ValueKind.DEFAULT_PARAMETER ->
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

    private fun rememberCapturedForDefaultLambda(defaultLambda: DefaultLambda) {
        for (captured in defaultLambda.capturedVars) {
            val info = invocationParamBuilder.addCapturedParam(captured, captured.fieldName, false)
            info.remapValue = StackValue.local(codegen.frameMap.enterTemp(info.type), info.type)
            info.isSynthetic = true
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
    }
}

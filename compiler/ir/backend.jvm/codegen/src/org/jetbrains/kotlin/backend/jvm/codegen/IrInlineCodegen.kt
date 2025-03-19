/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.backend.jvm.ir.isInlineOnly
import org.jetbrains.kotlin.backend.jvm.ir.isInlineParameter
import org.jetbrains.kotlin.backend.jvm.ir.unwrapInlineLambda
import org.jetbrains.kotlin.backend.jvm.localClassType
import org.jetbrains.kotlin.backend.jvm.mapping.IrCallableMethod
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.codegen.AsmUtil.genThrow
import org.jetbrains.kotlin.codegen.AsmUtil.isPrimitive
import org.jetbrains.kotlin.codegen.CompilationException
import org.jetbrains.kotlin.codegen.IrExpressionLambda
import org.jetbrains.kotlin.codegen.JvmKotlinType
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.ValueKind
import org.jetbrains.kotlin.codegen.inline.*
import org.jetbrains.kotlin.codegen.inline.CapturedParamInfo
import org.jetbrains.kotlin.codegen.inline.SMAPAndMethodNode
import org.jetbrains.kotlin.codegen.inline.nodeText
import org.jetbrains.kotlin.codegen.inline.preprocessSuspendMarkers
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.descriptors.toIrBasedKotlinType
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.isSuspendFunction
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.exceptions.rethrowIntellijPlatformExceptionIfNeeded
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.JumpInsnNode
import org.jetbrains.org.objectweb.asm.tree.LabelNode
import org.jetbrains.org.objectweb.asm.tree.LookupSwitchInsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import org.jetbrains.org.objectweb.asm.tree.TableSwitchInsnNode
import kotlin.math.max

class IrInlineCodegen(
    private val codegen: ExpressionCodegen,
    private val state: GenerationState,
    private val function: IrFunction,
    private val jvmSignature: JvmMethodSignature,
    private val typeParameterMappings: TypeParameterMappings<IrType>,
    private val sourceCompiler: SourceCompilerForInline,
    private val reifiedTypeInliner: ReifiedTypeInliner<IrType>,
) : IrInlineCallGenerator {

    private val inlineArgumentsInPlace = canInlineArgumentsInPlace()
    private val invocationParamBuilder = ParametersBuilder.newBuilder()
    private val maskValues = ArrayList<Int>()
    private var maskStartIndex = -1
    private var methodHandleInDefaultMethodIndex = -1
    private val initialFrameSize = codegen.frameMap.currentSize

    override fun genInlineCall(
        callableMethod: IrCallableMethod,
        codegen: ExpressionCodegen,
        expression: IrFunctionAccessExpression,
        isInsideIfCondition: Boolean,
    ) {
        var nodeAndSmap: SMAPAndMethodNode? = null
        try {
            nodeAndSmap = sourceCompiler.compileInlineFunction(jvmSignature).apply {
                node.preprocessSuspendMarkers(forInline = true, keepFakeContinuation = false)
            }
            val result = inlineCall(nodeAndSmap, function.isInlineOnly())
            leaveTemps()
            codegen.propagateChildReifiedTypeParametersUsages(result.reifiedTypeParametersUsages)
            codegen.markLineNumberAfterInlineIfNeeded(isInsideIfCondition)
            state.factory.removeClasses(result.calcClassesToRemove())
        } catch (e: CompilationException) {
            throw e
        } catch (e: InlineException) {
            throw CompilationException(
                "Couldn't inline method call: ${sourceCompiler.callElementText}",
                e, sourceCompiler.callElement as? PsiElement
            )
        } catch (e: Exception) {
            rethrowIntellijPlatformExceptionIfNeeded(e)
            throw CompilationException(
                "Couldn't inline method call: ${sourceCompiler.callElementText}\nMethod: ${nodeAndSmap?.node?.nodeText}",
                e, sourceCompiler.callElement as? PsiElement
            )
        }
    }

    override fun genCycleStub(text: String, codegen: ExpressionCodegen) {
        leaveTemps()
        genThrow(codegen.visitor, "java/lang/UnsupportedOperationException", "Call is part of inline cycle: $text")
    }

    override fun beforeCallStart() {
        if (inlineArgumentsInPlace) {
            codegen.visitor.addInplaceCallStartMarker()
        }
    }

    override fun afterCallEnd() {
        if (inlineArgumentsInPlace) {
            codegen.visitor.addInplaceCallEndMarker()
        }
    }

    override fun genValueAndPut(
        irValueParameter: IrValueParameter,
        argumentExpression: IrExpression,
        parameterType: Type,
        codegen: ExpressionCodegen,
        blockInfo: BlockInfo,
    ) {
        val inlineLambda = argumentExpression.unwrapInlineLambda()
        if (inlineLambda != null) {
            val lambdaInfo = IrExpressionLambdaImpl(codegen, inlineLambda)
            invocationParamBuilder.addNextValueParameter(
                parameterType,
                true,
                null,
                irValueParameter.indexInOldValueParameters
            ).functionalArgument = lambdaInfo
            lambdaInfo.generateLambdaBody(sourceCompiler)
            lambdaInfo.reference.getArgumentsWithIr().forEachIndexed { index, (_, ir) ->
                val param = lambdaInfo.capturedVars[index]
                val onStack = codegen.genOrGetLocal(ir, param.type, ir.type, BlockInfo(), eraseType = false)
                putCapturedToLocalVal(onStack, param, ir.type.toIrBasedKotlinType())
            }
        } else {
            val isInlineParameter = irValueParameter.isInlineParameter()
            val kind = when {
                irValueParameter.origin == IrDeclarationOrigin.MASK_FOR_DEFAULT_FUNCTION ->
                    ValueKind.DEFAULT_MASK
                irValueParameter.origin == IrDeclarationOrigin.METHOD_HANDLER_IN_DEFAULT_FUNCTION ->
                    ValueKind.METHOD_HANDLE_IN_DEFAULT
                argumentExpression is IrContainerExpression && argumentExpression.origin == IrStatementOrigin.DEFAULT_VALUE ->
                    if (isInlineParameter)
                        ValueKind.DEFAULT_INLINE_PARAMETER
                    else
                        ValueKind.DEFAULT_PARAMETER
                isInlineParameter && irValueParameter.type.isSuspendFunction() ->
                    if (argumentExpression.isReadOfInlineLambda())
                        ValueKind.READ_OF_INLINE_LAMBDA_FOR_INLINE_SUSPEND_PARAMETER
                    else
                        ValueKind.READ_OF_OBJECT_FOR_INLINE_SUSPEND_PARAMETER
                else ->
                    ValueKind.GENERAL
            }

            val onStack = when (kind) {
                ValueKind.METHOD_HANDLE_IN_DEFAULT ->
                    StackValue.Constant(null, AsmTypes.OBJECT_TYPE)
                ValueKind.DEFAULT_MASK ->
                    StackValue.Constant((argumentExpression as IrConst).value, Type.INT_TYPE)
                ValueKind.DEFAULT_PARAMETER, ValueKind.DEFAULT_INLINE_PARAMETER ->
                    StackValue.createDefaultValue(parameterType)
                else -> {
                    if (inlineArgumentsInPlace) {
                        codegen.visitor.addInplaceArgumentStartMarker()
                    }
                    // Here we replicate the old backend: reusing the locals for everything except extension receivers.
                    // TODO when stopping at a breakpoint placed in an inline function, arguments which reuse an existing
                    //   local will not be visible in the debugger, so this needs to be reconsidered.
                    val argValue = if (irValueParameter.indexInOldValueParameters >= 0) {
                        codegen.genOrGetLocal(argumentExpression, parameterType, irValueParameter.type, blockInfo, eraseType = true)
                    } else {
                        codegen.gen(argumentExpression, parameterType, irValueParameter.type, blockInfo)
                        StackValue.OnStack(parameterType, irValueParameter.type.toIrBasedKotlinType())
                    }
                    // StackValue.Local hasn't materialized on the stack yet, postpone adding END marker
                    if (inlineArgumentsInPlace && argValue !is StackValue.Local) {
                        codegen.visitor.addInplaceArgumentEndMarker()
                    }
                    argValue
                }
            }

            fun addInplaceArgumentEndMarkerIfPostponed() {
                if (onStack is StackValue.Local) codegen.visitor.addInplaceArgumentEndMarker()
            }

            val expectedType = JvmKotlinType(parameterType, irValueParameter.type.toIrBasedKotlinType())
            val parameterIndex = irValueParameter.indexInOldValueParameters

            if (kind === ValueKind.DEFAULT_MASK || kind === ValueKind.METHOD_HANDLE_IN_DEFAULT) {
                assert(onStack is StackValue.Constant) { "Additional default method argument should be constant, but $onStack" }
                val constantValue = (onStack as StackValue.Constant).value
                if (kind === ValueKind.DEFAULT_MASK) {
                    assert(constantValue is Int) { "Mask should be of Integer type, but $constantValue" }
                    maskValues.add(constantValue as Int)
                    if (maskStartIndex == -1) {
                        maskStartIndex = invocationParamBuilder.listAllParams().sumOf<ParameterInfo> {
                            if (it is CapturedParamInfo) 0 else it.type.size
                        }
                    }
                } else {
                    assert(constantValue == null) { "Additional method handle for default argument should be null, but " + constantValue!! }
                    methodHandleInDefaultMethodIndex = maskStartIndex + maskValues.size
                }
                return
            }

            val info = when (parameterIndex) {
                -1 -> invocationParamBuilder.addNextParameter(expectedType.type, false)
                else -> invocationParamBuilder.addNextValueParameter(expectedType.type, false, null, parameterIndex)
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
                kind === ValueKind.DEFAULT_PARAMETER || kind === ValueKind.DEFAULT_INLINE_PARAMETER -> {
                    codegen.frameMap.enterTemp(info.type) // the inline function will put the value into this slot
                    addInplaceArgumentEndMarkerIfPostponed()
                }
                onStack.isLocalWithNoBoxing(expectedType) -> {
                    info.remapValue = onStack
                    addInplaceArgumentEndMarkerIfPostponed()
                }
                else -> {
                    onStack.put(info.type, expectedType.kotlinType, codegen.visitor)
                    addInplaceArgumentEndMarkerIfPostponed()
                    codegen.visitor.store(codegen.frameMap.enterTemp(info.type), info.type)
                }
            }
        }
    }

    private fun isInlinedToInlineFunInKotlinRuntime(): Boolean {
        val callee = codegen.irFunction
        return callee.isInline && callee.getPackageFragment().packageFqName.startsWith(StandardNames.BUILT_INS_PACKAGE_NAME)
    }

    private fun canInlineArgumentsInPlace(): Boolean {
        if (!function.isInlineOnly())
            return false

        var actualParametersCount = function.valueParameters.size
        if (function.dispatchReceiverParameter != null)
            ++actualParametersCount
        if (function.extensionReceiverParameter != null)
            ++actualParametersCount
        if (actualParametersCount == 0)
            return false

        if (function.valueParameters.any { it.isInlineParameter() })
            return false

        return canInlineArgumentsInPlace(sourceCompiler.compileInlineFunction(jvmSignature).node)
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
                    param.remapValue = StackValue.Local(codegen.frameMap.enterTemp(param.type), param.type, null)
                    param.isSynthetic = true
                }
            }
        }

        val reificationResult = reifiedTypeInliner.reifyInstructions(node)

        val parameters = invocationParamBuilder.buildParameters()

        val info = RootInliningContext(
            state, codegen.inlineNameGenerator.subGenerator(jvmSignature.asmMethod.name),
            sourceCompiler, sourceCompiler.inlineCallSiteInfo, reifiedTypeInliner, typeParameterMappings,
            codegen.inlineScopesGenerator
        )

        val sourceMapper = sourceCompiler.sourceMapper
        val sourceInfo = sourceMapper.sourceInfo!!
        val lastLineNumber = codegen.lastLineNumber
        val callSite = SourcePosition(lastLineNumber, sourceInfo.sourceFileName!!, sourceInfo.pathOrCleanFQN)
        info.inlineScopesGenerator?.apply { currentCallSiteLineNumber = lastLineNumber }
        val inliner = MethodInliner(
            node, parameters, info, FieldRemapper(null, null, parameters), sourceCompiler.isCallInsideSameModuleAsCallee,
            { "Method inlining " + sourceCompiler.callElementText },
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
            // May be inlining code into `<clinit>`, in which case it's too late to modify the IR and
            // `generateAssertFieldIfNeeded` will return a statement for which we need to emit bytecode.
            val isClInit = sourceCompiler.inlineCallSiteInfo.method.name == "<clinit>"
            codegen.classCodegen.generateAssertFieldIfNeeded(isClInit)?.accept(codegen, BlockInfo())?.discard()
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
        offsetForFinallyLocalVar: Int,
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

    private fun leaveTemps() {
        invocationParamBuilder.listAllParams().asReversed().forEach { param ->
            if (!param.isSkippedOrRemapped || CapturedParamInfo.isSynthetic(param)) {
                codegen.frameMap.leaveTemp(param.type)
            }
        }
    }

    private fun putCapturedToLocalVal(stackValue: StackValue, capturedParam: CapturedParamDesc, kotlinType: KotlinType?) {
        val info = invocationParamBuilder.addCapturedParam(capturedParam, capturedParam.fieldName, false)
        val asmType = info.type
        if (stackValue.isLocalWithNoBoxing(JvmKotlinType(asmType, kotlinType))) {
            info.remapValue = stackValue
        } else {
            stackValue.put(asmType, kotlinType, codegen.visitor)
            val index = codegen.frameMap.enterTemp(asmType)
            codegen.visitor.store(index, asmType)
            info.remapValue = StackValue.Local(index, asmType, null)
            info.isSynthetic = true
        }
    }

    companion object {
        private fun StackValue.isLocalWithNoBoxing(expected: JvmKotlinType): Boolean =
            this is StackValue.Local &&
                    isPrimitive(expected.type) == isPrimitive(type) &&
                    !StackValue.requiresInlineClassBoxingOrUnboxing(type, kotlinType, expected.type, expected.kotlinType)

        // Stack spilling before inline function call is required if the inlined bytecode has:
        //   1. try-catch blocks - otherwise the stack spilling before and after them will not be correct;
        //   2. suspension points - again, the stack spilling around them is otherwise wrong;
        //   3. loops - OpenJDK cannot JIT-optimize between loop iterations if the stack is not empty.
        // Instead of checking for loops precisely, we just check if there are any backward jumps -
        // that is, a jump from instruction #i to instruction #j where j < i.
        private fun MethodNode.requiresEmptyStackOnEntry(): Boolean = tryCatchBlocks.isNotEmpty() ||
                instructions.any { isBeforeSuspendMarker(it) || isBeforeInlineSuspendMarker(it) || isBackwardsJump(it) }

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

class IrExpressionLambdaImpl(
    codegen: ExpressionCodegen,
    val reference: IrFunctionReference,
) : ExpressionLambda(), IrExpressionLambda {
    override val isExtensionLambda: Boolean = function.extensionReceiverParameter != null && reference.extensionReceiver == null

    override val contextParameterCount: Int = function.parameters.count { it.kind == IrParameterKind.Context }

    val function: IrFunction
        get() = reference.symbol.owner

    override val hasDispatchReceiver: Boolean
        get() = false

    // This name doesn't actually matter: it is used internally to tell this lambda's captured
    // arguments apart from any other scope's. So long as it's unique, any value is fine.
    // This particular string slightly aids in debugging internal compiler errors as it at least
    // points towards the function containing the lambda.
    override val lambdaClassType: Type = reference.localClassType
        ?: throw AssertionError("callable reference ${reference.dump()} has no name in context")

    override val capturedVars: List<CapturedParamDesc>
    override val invokeMethod: Method
    override val invokeMethodParameters: List<KotlinType?>
    override val invokeMethodReturnType: KotlinType

    init {
        val asmMethod = codegen.methodSignatureMapper.mapAsmMethod(function)
        val capturedParameters = reference.getArgumentsWithIr()
        val captureStart =
            contextParameterCount + if (isExtensionLambda) 1 else 0 // extension receiver comes before captures
        val captureEnd = captureStart + capturedParameters.size
        capturedVars = capturedParameters.mapIndexed { index, (parameter, _) ->
            val isSuspend = parameter.isInlineParameter() && parameter.type.isSuspendFunction()
            capturedParamDesc(parameter.name.asString(), asmMethod.argumentTypes[captureStart + index], isSuspend)
        }
        // The parameter list should include the continuation if this is a suspend lambda. In the IR backend,
        // the lambda is suspend iff the inline function's parameter is marked suspend, so FunctionN.invoke call
        // inside the inline function already has a (real) continuation value as the last argument.
        val freeParameters = function.parameters.let { it.take(captureStart) + it.drop(captureEnd) }
        val freeAsmParameters = asmMethod.argumentTypes.let { it.take(captureStart) + it.drop(captureEnd) }
        // The return type, on the other hand, should be the original type if this is a suspend lambda that returns
        // an unboxed inline class value so that the inliner will box it (FunctionN.invoke should return a boxed value).
        val unboxedReturnType = function.originalReturnTypeOfSuspendFunctionReturningUnboxedInlineClass()
        val unboxedAsmReturnType = unboxedReturnType?.let(codegen.typeMapper::mapType)
        invokeMethod = Method(asmMethod.name, unboxedAsmReturnType ?: asmMethod.returnType, freeAsmParameters.toTypedArray())
        invokeMethodParameters = freeParameters.map { it.type.toIrBasedKotlinType() }
        invokeMethodReturnType = (unboxedReturnType ?: function.returnType).toIrBasedKotlinType()
    }
}

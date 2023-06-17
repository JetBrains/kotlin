/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.jvm.ir.isInlineOnly
import org.jetbrains.kotlin.backend.jvm.ir.isInlineParameter
import org.jetbrains.kotlin.backend.jvm.ir.unwrapInlineLambda
import org.jetbrains.kotlin.backend.jvm.mapping.IrCallableMethod
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.codegen.IrExpressionLambda
import org.jetbrains.kotlin.codegen.JvmKotlinType
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.ValueKind
import org.jetbrains.kotlin.codegen.inline.*
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.descriptors.toIrBasedKotlinType
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method

class IrInlineCodegen(
    codegen: ExpressionCodegen,
    state: GenerationState,
    private val function: IrFunction,
    signature: JvmMethodSignature,
    typeParameterMappings: TypeParameterMappings<IrType>,
    sourceCompiler: SourceCompilerForInline,
    reifiedTypeInliner: ReifiedTypeInliner<IrType>
) :
    InlineCodegen<ExpressionCodegen>(codegen, state, signature, typeParameterMappings, sourceCompiler, reifiedTypeInliner),
    IrInlineCallGenerator {

    private val inlineArgumentsInPlace = canInlineArgumentsInPlace()

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

    override fun generateAssertField() {
        // May be inlining code into `<clinit>`, in which case it's too late to modify the IR and
        // `generateAssertFieldIfNeeded` will return a statement for which we need to emit bytecode.
        val isClInit = sourceCompiler.inlineCallSiteInfo.method.name == "<clinit>"
        codegen.classCodegen.generateAssertFieldIfNeeded(isClInit)?.accept(codegen, BlockInfo())?.discard()
    }

    override fun isInlinedToInlineFunInKotlinRuntime(): Boolean {
        val callee = codegen.irFunction
        return callee.isInline && callee.getPackageFragment().packageFqName.startsWith(StandardNames.BUILT_INS_PACKAGE_NAME)
    }

    override fun genValueAndPut(
        irValueParameter: IrValueParameter,
        argumentExpression: IrExpression,
        parameterType: Type,
        codegen: ExpressionCodegen,
        blockInfo: BlockInfo
    ) {
        val inlineLambda = argumentExpression.unwrapInlineLambda()
        if (inlineLambda != null) {
            val lambdaInfo = IrExpressionLambdaImpl(codegen, inlineLambda)
            rememberClosure(parameterType, irValueParameter.index, lambdaInfo)
            lambdaInfo.generateLambdaBody(sourceCompiler)
            lambdaInfo.reference.getArgumentsWithIr().forEachIndexed { index, (_, ir) ->
                val param = lambdaInfo.capturedVars[index]
                val onStack = codegen.genOrGetLocal(ir, param.type, ir.type, BlockInfo())
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
                    StackValue.constant(null, AsmTypes.OBJECT_TYPE)
                ValueKind.DEFAULT_MASK ->
                    StackValue.constant((argumentExpression as IrConst<*>).value, Type.INT_TYPE)
                ValueKind.DEFAULT_PARAMETER, ValueKind.DEFAULT_INLINE_PARAMETER ->
                    StackValue.createDefaultValue(parameterType)
                else -> {
                    if (inlineArgumentsInPlace) {
                        codegen.visitor.addInplaceArgumentStartMarker()
                    }
                    // Here we replicate the old backend: reusing the locals for everything except extension receivers.
                    // TODO when stopping at a breakpoint placed in an inline function, arguments which reuse an existing
                    //   local will not be visible in the debugger, so this needs to be reconsidered.
                    val argValue = if (irValueParameter.index >= 0)
                        codegen.genOrGetLocal(argumentExpression, parameterType, irValueParameter.type, blockInfo)
                    else
                        codegen.genToStackValue(argumentExpression, parameterType, irValueParameter.type, blockInfo)
                    if (inlineArgumentsInPlace) {
                        codegen.visitor.addInplaceArgumentEndMarker()
                    }
                    argValue
                }
            }

            val expectedType = JvmKotlinType(parameterType, irValueParameter.type.toIrBasedKotlinType())
            putArgumentToLocalVal(expectedType, onStack, irValueParameter.index, kind)
        }
    }

    override fun genInlineCall(
        callableMethod: IrCallableMethod,
        codegen: ExpressionCodegen,
        expression: IrFunctionAccessExpression,
        isInsideIfCondition: Boolean,
    ) {
        performInline(isInsideIfCondition, function.isInlineOnly())
    }

    override fun genCycleStub(text: String, codegen: ExpressionCodegen) {
        generateStub(text, codegen)
    }
}

class IrExpressionLambdaImpl(
    codegen: ExpressionCodegen,
    val reference: IrFunctionReference,
) : ExpressionLambda(), IrExpressionLambda {
    override val isExtensionLambda: Boolean = function.extensionReceiverParameter != null && reference.extensionReceiver == null

    val function: IrFunction
        get() = reference.symbol.owner

    override val hasDispatchReceiver: Boolean
        get() = false

    // This name doesn't actually matter: it is used internally to tell this lambda's captured
    // arguments apart from any other scope's. So long as it's unique, any value is fine.
    // This particular string slightly aids in debugging internal compiler errors as it at least
    // points towards the function containing the lambda.
    override val lambdaClassType: Type = codegen.context.getLocalClassType(reference)
        ?: throw AssertionError("callable reference ${reference.dump()} has no name in context")

    override val capturedVars: List<CapturedParamDesc>
    override val invokeMethod: Method
    override val invokeMethodParameters: List<KotlinType?>
    override val invokeMethodReturnType: KotlinType

    init {
        val asmMethod = codegen.methodSignatureMapper.mapAsmMethod(function)
        val capturedParameters = reference.getArgumentsWithIr()
        val captureStart = if (isExtensionLambda) 1 else 0 // extension receiver comes before captures
        val captureEnd = captureStart + capturedParameters.size
        capturedVars = capturedParameters.mapIndexed { index, (parameter, _) ->
            val isSuspend = parameter.isInlineParameter() && parameter.type.isSuspendFunction()
            capturedParamDesc(parameter.name.asString(), asmMethod.argumentTypes[captureStart + index], isSuspend)
        }
        // The parameter list should include the continuation if this is a suspend lambda. In the IR backend,
        // the lambda is suspend iff the inline function's parameter is marked suspend, so FunctionN.invoke call
        // inside the inline function already has a (real) continuation value as the last argument.
        val freeParameters = function.explicitParameters.let { it.take(captureStart) + it.drop(captureEnd) }
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

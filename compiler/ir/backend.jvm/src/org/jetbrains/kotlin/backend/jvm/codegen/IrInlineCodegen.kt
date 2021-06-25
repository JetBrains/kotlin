/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.isInlineClassType
import org.jetbrains.kotlin.backend.jvm.ir.isInlineParameter
import org.jetbrains.kotlin.backend.jvm.lower.inlineclasses.InlineClassAbi
import org.jetbrains.kotlin.codegen.IrExpressionLambda
import org.jetbrains.kotlin.codegen.JvmKotlinType
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.ValueKind
import org.jetbrains.kotlin.codegen.inline.*
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.descriptors.toIrBasedKotlinType
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method
import org.jetbrains.org.objectweb.asm.tree.MethodNode

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

    override fun generateAssertFieldIfNeeded(info: RootInliningContext) {
        if (info.generateAssertField) {
            // May be inlining code into `<clinit>`, in which case it's too late to modify the IR and
            // `generateAssertFieldIfNeeded` will return a statement for which we need to emit bytecode.
            val isClInit = info.callSiteInfo.method.name == "<clinit>"
            codegen.classCodegen.generateAssertFieldIfNeeded(isClInit)?.accept(codegen, BlockInfo())?.discard()
        }
    }

    override fun genValueAndPut(
        irValueParameter: IrValueParameter,
        argumentExpression: IrExpression,
        parameterType: Type,
        codegen: ExpressionCodegen,
        blockInfo: BlockInfo
    ) {
        val isInlineParameter = irValueParameter.isInlineParameter()
        if (isInlineParameter && argumentExpression.isInlineIrExpression()) {
            val irReference = (argumentExpression as IrBlock).statements.filterIsInstance<IrFunctionReference>().single()
            val lambdaInfo = IrExpressionLambdaImpl(codegen, irReference, irValueParameter)
            rememberClosure(parameterType, irValueParameter.index, lambdaInfo)
            lambdaInfo.generateLambdaBody(sourceCompiler)
            lambdaInfo.reference.getArgumentsWithIr().forEachIndexed { index, (_, ir) ->
                val param = lambdaInfo.capturedVars[index]
                val onStack = codegen.genOrGetLocal(ir, param.type, ir.type, BlockInfo())
                putCapturedToLocalVal(onStack, param, ir.type.toIrBasedKotlinType())
            }
        } else {
            val kind = when (irValueParameter.origin) {
                IrDeclarationOrigin.MASK_FOR_DEFAULT_FUNCTION -> ValueKind.DEFAULT_MASK
                IrDeclarationOrigin.METHOD_HANDLER_IN_DEFAULT_FUNCTION -> ValueKind.METHOD_HANDLE_IN_DEFAULT
                else -> when {
                    argumentExpression is IrContainerExpression && argumentExpression.origin == IrStatementOrigin.DEFAULT_VALUE ->
                        ValueKind.DEFAULT_PARAMETER
                    // TODO ValueKind.NON_INLINEABLE_ARGUMENT_FOR_INLINE_PARAMETER_CALLED_IN_SUSPEND?
                    isInlineParameter && irValueParameter.type.isSuspendFunctionTypeOrSubtype() ->
                        ValueKind.NON_INLINEABLE_ARGUMENT_FOR_INLINE_SUSPEND_PARAMETER
                    else -> ValueKind.GENERAL
                }
            }

            val onStack = when {
                kind == ValueKind.METHOD_HANDLE_IN_DEFAULT -> StackValue.constant(null, AsmTypes.OBJECT_TYPE)
                kind == ValueKind.DEFAULT_MASK -> StackValue.constant((argumentExpression as IrConst<*>).value, Type.INT_TYPE)
                kind == ValueKind.DEFAULT_PARAMETER -> StackValue.createDefaultValue(parameterType)
                irValueParameter.index >= 0
                    // Reuse an existing local if possible. NOTE: when stopping at a breakpoint placed
                    // in an inline function, arguments which reuse an existing local will not be visible
                    // in the debugger.
                -> codegen.genOrGetLocal(argumentExpression, parameterType, irValueParameter.type, blockInfo)
                else
                    // Do not reuse locals for receivers. While it's actually completely fine, the non-IR
                    // backend does not do it for internal reasons, and here we replicate the debugging
                    // experience.
                -> codegen.gen(argumentExpression, parameterType, irValueParameter.type, blockInfo)
            }

            val expectedType = JvmKotlinType(parameterType, irValueParameter.type.toIrBasedKotlinType())
            putArgumentToLocalVal(expectedType, onStack, irValueParameter.index, kind)
        }
    }

    override fun beforeValueParametersStart() {
        invocationParamBuilder.markValueParametersStart()
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

    override fun extractDefaultLambdas(node: MethodNode): List<DefaultLambda> =
        extractDefaultLambdas(node, extractDefaultLambdaOffsetAndDescriptor(jvmSignature, function)) { parameter ->
            IrDefaultLambda(type, capturedArgs, parameter, offset, needReification, sourceCompiler as IrSourceCompilerForInline)
        }
}

class IrExpressionLambdaImpl(
    codegen: ExpressionCodegen,
    val reference: IrFunctionReference,
    irValueParameter: IrValueParameter
) : ExpressionLambda(irValueParameter.isCrossinline), IrExpressionLambda {
    override val isExtensionLambda: Boolean = irValueParameter.type.isExtensionFunctionType

    val function: IrFunction
        get() = reference.symbol.owner

    override val isBoundCallableReference: Boolean
        get() = reference.extensionReceiver != null

    override val isSuspend: Boolean
        get() = reference.symbol.owner.isSuspend

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
        val (startCapture, endCapture) = when {
            isBoundCallableReference -> 0 to 1 // (bound receiver, real parameters...)
            isExtensionLambda -> 1 to capturedParameters.size + 1 // (unbound receiver, captures..., real parameters...)
            else -> 0 to capturedParameters.size // (captures..., real parameters...)
        }
        capturedVars = capturedParameters.mapIndexed { index, (parameter, _) ->
            val isSuspend = parameter.isInlineParameter() && parameter.type.isSuspendFunctionTypeOrSubtype()
            capturedParamDesc(parameter.name.asString(), asmMethod.argumentTypes[startCapture + index], isSuspend)
        }
        // The parameter list should include the continuation if this is a suspend lambda. In the IR backend,
        // the lambda is suspend iff the inline function's parameter is marked suspend, so FunctionN.invoke call
        // inside the inline function already has a (real) continuation value as the last argument.
        val freeParameters = function.explicitParameters.let { it.take(startCapture) + it.drop(endCapture) }
        val freeAsmParameters = asmMethod.argumentTypes.let { it.take(startCapture) + it.drop(endCapture) }
        // The return type, on the other hand, should be the original type if this is a suspend lambda that returns
        // an unboxed inline class value so that the inliner will box it (FunctionN.invoke should return a boxed value).
        val unboxedReturnType = function.originalReturnTypeOfSuspendFunctionReturningUnboxedInlineClass()
        val unboxedAsmReturnType = unboxedReturnType?.let(codegen.typeMapper::mapType)
        invokeMethod = Method(asmMethod.name, unboxedAsmReturnType ?: asmMethod.returnType, freeAsmParameters.toTypedArray())
        invokeMethodParameters = freeParameters.map { it.type.toIrBasedKotlinType() }
        invokeMethodReturnType = (unboxedReturnType ?: function.returnType).toIrBasedKotlinType()
    }
}

class IrDefaultLambda(
    lambdaClassType: Type,
    capturedArgs: Array<Type>,
    irValueParameter: IrValueParameter,
    offset: Int,
    needReification: Boolean,
    sourceCompiler: IrSourceCompilerForInline
) : DefaultLambda(lambdaClassType, capturedArgs, irValueParameter.isCrossinline, offset, needReification, sourceCompiler) {

    private val typeArguments: MutableList<IrType>

    override val invokeMethodParameters: List<KotlinType>
        get() = typeArguments.dropLast(1).map { it.toIrBasedKotlinType() }

    override val invokeMethodReturnType: KotlinType
        get() = typeArguments.last().toIrBasedKotlinType()

    init {
        val context = sourceCompiler.codegen.context

        typeArguments =
            (irValueParameter.type as IrSimpleType).arguments
                .mapTo(mutableListOf()) {
                    when (it) {
                        is IrTypeProjection -> it.type
                        else -> context.irBuiltIns.anyNType
                    }
                }
                .apply {
                    // Suspend function references: `suspend (A) -> B` => `invoke(A, Continuation<B>): Any?`
                    // TODO: default suspend lambdas are currently uninlinable due to having a state machine
                    if (irValueParameter.type.isSuspendFunction()) {
                        set(size - 1, context.ir.symbols.continuationClass.typeWith(get(size - 1)))
                        add(context.irBuiltIns.anyNType)
                    }
                }

        val base = if (isPropertyReference) OperatorNameConventions.GET.asString() else OperatorNameConventions.INVOKE.asString()
        val name = InlineClassAbi.hashSuffix(
            context.state.useOldManglingSchemeForFunctionsWithInlineClassesInSignatures,
            typeArguments.dropLast(1),
            typeArguments.last().takeIf { it.isInlineClassType() }
        )?.let { "$base-$it" } ?: base
        // TODO: while technically only the number of arguments here matters right now (see `loadInvoke`),
        //       it would be better to map to a non-erased signature if not a property reference.
        if (loadInvoke(sourceCompiler, base, Method(name, AsmTypes.OBJECT_TYPE, Array(typeArguments.size - 1) { AsmTypes.OBJECT_TYPE }))) {
            // If the loaded method is `invoke(Object, ...) -> Object`, then it expects boxed parameters and returns a boxed value.
            typeArguments.replaceAll { context.irBuiltIns.anyNType }
        }
    }
}

fun IrExpression.isInlineIrExpression() =
    when (this) {
        is IrBlock -> origin.isInlineIrExpression()
        is IrCallableReference<*> -> true.also {
            assert((0 until valueArgumentsCount).count { getValueArgument(it) != null } == 0) {
                "Expecting 0 value arguments for bounded callable reference: ${dump()}"
            }
        }
        else -> false
    }

fun IrStatementOrigin?.isInlineIrExpression() =
    isLambda || this == IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE || this == IrStatementOrigin.SUSPEND_CONVERSION

fun IrFunction.isInlineFunctionCall(context: JvmBackendContext) =
    (!context.state.isInlineDisabled || typeParameters.any { it.isReified }) && (isInline || isInlineArrayConstructor(context))

// Constructors can't be marked as inline in metadata, hence this hack.
private fun IrFunction.isInlineArrayConstructor(context: JvmBackendContext): Boolean =
    this is IrConstructor && valueParameters.size == 2 && constructedClass.symbol.let {
        it == context.irBuiltIns.arrayClass || it in context.irBuiltIns.primitiveArrays
    }

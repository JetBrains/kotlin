/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.IrBuildingTransformer
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.codegen.fileParent
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.erasedUpperBound
import org.jetbrains.kotlin.backend.jvm.ir.getKtFile
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.isInlined
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.org.objectweb.asm.Handle
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.commons.Method
import java.lang.invoke.LambdaMetafactory

// After this pass runs there are only four kinds of IrTypeOperatorCalls left:
//
// - IMPLICIT_CAST
// - SAFE_CAST with reified type parameters
// - INSTANCEOF with non-nullable type operand or reified type parameters
// - CAST with non-nullable argument, nullable type operand, or reified type parameters
//
// The latter two correspond to the instanceof/checkcast instructions on the JVM, except for
// the presence of reified type parameters.
internal val typeOperatorLowering = makeIrFilePhase(
    ::TypeOperatorLowering,
    name = "TypeOperatorLowering",
    description = "Lower IrTypeOperatorCalls to (implicit) casts and instanceof checks"
)

private class TypeOperatorLowering(private val context: JvmBackendContext) : FileLoweringPass, IrBuildingTransformer(context) {
    override fun lower(irFile: IrFile) = irFile.transformChildrenVoid()

    private fun IrExpression.transformVoid() = transform(this@TypeOperatorLowering, null)

    private val IrType.isReifiedTypeParameter: Boolean
        get() = classifierOrNull?.safeAs<IrTypeParameterSymbol>()?.owner?.isReified == true

    private fun lowerInstanceOf(argument: IrExpression, type: IrType) = with(builder) {
        when {
            type.isReifiedTypeParameter ->
                irIs(argument, type)
            argument.type.isNullable() && type.isNullable() -> {
                irLetS(argument, irType = context.irBuiltIns.anyNType) { valueSymbol ->
                    context.oror(
                        irEqualsNull(irGet(valueSymbol.owner)),
                        irIs(irGet(valueSymbol.owner), type.makeNotNull())
                    )
                }
            }
            argument.type.isNullable() && !type.isNullable() && argument.type.erasedUpperBound == type.erasedUpperBound ->
                irNotEquals(argument, irNull())
            else -> irIs(argument, type.makeNotNull())
        }
    }

    private fun lowerCast(argument: IrExpression, type: IrType): IrExpression = when {
        type.isReifiedTypeParameter ->
            builder.irAs(argument, type)
        argument.type.isNullable() && !type.isNullable() ->
            with(builder) {
                irLetS(argument, irType = context.irBuiltIns.anyNType) { valueSymbol ->
                    irIfNull(
                        type,
                        irGet(valueSymbol.owner),
                        irCall(throwTypeCastException).apply {
                            putValueArgument(0, irString("null cannot be cast to non-null type ${type.render()}"))
                        },
                        lowerCast(irGet(valueSymbol.owner), type.makeNullable())
                    )
                }
            }
        argument.type.isSubtypeOfClass(type.erasedUpperBound.symbol) ->
            argument
        else ->
            builder.irAs(argument, type)
    }

    private val jvmIndyLambdaMetafactoryIntrinsic = context.ir.symbols.indyLambdaMetafactoryIntrinsic

    private val indyIntrinsic = context.ir.symbols.jvmIndyIntrinsic

    private fun IrBuilderWithScope.jvmInvokeDynamic(
        dynamicCall: IrCall,
        bootstrapMethod: Handle,
        bootstrapMethodArguments: List<IrExpression>
    ) =
        irCall(indyIntrinsic, dynamicCall.type).apply {
            putTypeArgument(0, dynamicCall.type)
            putValueArgument(0, dynamicCall)
            putValueArgument(1, irInt(bootstrapMethod.tag))
            putValueArgument(2, irString(bootstrapMethod.owner))
            putValueArgument(3, irString(bootstrapMethod.name))
            putValueArgument(4, irString(bootstrapMethod.desc))
            putValueArgument(5, irVararg(context.irBuiltIns.anyType, bootstrapMethodArguments))
        }

    private val originalMethodTypeIntrinsic = context.ir.symbols.jvmOriginalMethodTypeIntrinsic
    private val substitutedMethodTypeIntrinsic = context.ir.symbols.jvmSubstitutedMethodTypeIntrinsic

    private fun IrBuilderWithScope.jvmOriginalMethodType(methodSymbol: IrFunctionSymbol) =
        irCall(originalMethodTypeIntrinsic, context.irBuiltIns.anyType).apply {
            putValueArgument(0, irRawFunctionReferefence(context.irBuiltIns.anyType, methodSymbol))
        }

    @Suppress("unused")
    private fun IrBuilderWithScope.jvmSubstitutedMethodType(ownerType: IrType, methodSymbol: IrFunctionSymbol) =
        irCall(substitutedMethodTypeIntrinsic, context.irBuiltIns.anyType).apply {
            putTypeArgument(0, ownerType)
            putValueArgument(0, irRawFunctionReferefence(context.irBuiltIns.anyType, methodSymbol))
        }

    /**
     * @see java.lang.invoke.LambdaMetafactory.metafactory
     */
    private val jdkMetafactoryHandle =
        Handle(
            Opcodes.H_INVOKESTATIC,
            "java/lang/invoke/LambdaMetafactory",
            "metafactory",
            "(" +
                    "Ljava/lang/invoke/MethodHandles\$Lookup;" +
                    "Ljava/lang/String;" +
                    "Ljava/lang/invoke/MethodType;" +
                    "Ljava/lang/invoke/MethodType;" +
                    "Ljava/lang/invoke/MethodHandle;" +
                    "Ljava/lang/invoke/MethodType;" +
                    ")Ljava/lang/invoke/CallSite;",
            false
        )

    /**
     * @see java.lang.invoke.LambdaMetafactory.altMetafactory
     */
    private val jdkAltMetafactoryHandle =
        Handle(
            Opcodes.H_INVOKESTATIC,
            "java/lang/invoke/LambdaMetafactory",
            "altMetafactory",
            "(" +
                    "Ljava/lang/invoke/MethodHandles\$Lookup;" +
                    "Ljava/lang/String;" +
                    "Ljava/lang/invoke/MethodType;" +
                    "[Ljava/lang/Object;" +
                    ")Ljava/lang/invoke/CallSite;",
            false
        )

    override fun visitCall(expression: IrCall): IrExpression {
        return when (expression.symbol) {
            jvmIndyLambdaMetafactoryIntrinsic -> {
                expression.transformChildrenVoid()
                rewriteIndyLambdaMetafactoryCall(expression)
            }
            else -> super.visitCall(expression)
        }
    }

    /**
     * @see FunctionReferenceLowering.wrapWithIndySamConversion
     */
    private fun rewriteIndyLambdaMetafactoryCall(call: IrCall): IrCall {
        fun fail(message: String): Nothing =
            throw AssertionError("$message, call:\n${call.dump()}")

        val startOffset = call.startOffset
        val endOffset = call.endOffset

        val samType = call.getTypeArgument(0) as? IrSimpleType
            ?: fail("'samType' is expected to be a simple type")

        val samMethodRef = call.getValueArgument(0) as? IrRawFunctionReference
            ?: fail("'samMethodType' should be 'IrRawFunctionReference'")
        val implFunRef = call.getValueArgument(1) as? IrFunctionReference
            ?: fail("'implMethodReference' is expected to be 'IrFunctionReference'")
        val implFunSymbol = implFunRef.symbol
        val instanceMethodRef = call.getValueArgument(2) as? IrRawFunctionReference
            ?: fail("'instantiatedMethodType' is expected to be 'IrRawFunctionReference'")

        val extraOverriddenMethods = run {
            val extraOverriddenMethodVararg = call.getValueArgument(3) as? IrVararg
                ?: fail("'extraOverriddenMethodTypes' is expected to be 'IrVararg'")
            extraOverriddenMethodVararg.elements.map {
                val ref = it as? IrRawFunctionReference
                    ?: fail("'extraOverriddenMethodTypes' elements are expected to be 'IrRawFunctionReference'")
                ref.symbol.owner as? IrSimpleFunction
                    ?: fail("Extra overridden method is expected to be 'IrSimpleFunction': ${ref.symbol.owner.render()}")
            }
        }

        val samMethod = samMethodRef.symbol.owner as? IrSimpleFunction
            ?: fail("SAM method is expected to be 'IrSimpleFunction': ${samMethodRef.symbol.owner.render()}")
        val instanceMethod = instanceMethodRef.symbol.owner as? IrSimpleFunction
            ?: fail("Instance method is expected to be 'IrSimpleFunction': ${instanceMethodRef.symbol.owner.render()}")

        val dynamicCall = wrapClosureInDynamicCall(samType, samMethod, implFunRef)

        val requiredBridges = getOverriddenMethodsRequiringBridges(instanceMethod, samMethod, extraOverriddenMethods)

        return context.createJvmIrBuilder(implFunSymbol, startOffset, endOffset).run {
            val samMethodType = jvmOriginalMethodType(samMethod.symbol)
            val irRawFunRef = irRawFunctionReferefence(implFunRef.type, implFunSymbol)
            val instanceMethodType = jvmOriginalMethodType(instanceMethodRef.symbol)

            if (requiredBridges.isNotEmpty()) {
                val bridgeMethodTypes = requiredBridges.map { jvmOriginalMethodType(it.symbol) }
                jvmInvokeDynamic(
                    dynamicCall,
                    jdkAltMetafactoryHandle,
                    listOf(
                        samMethodType, irRawFunRef, instanceMethodType,
                        irInt(LambdaMetafactory.FLAG_BRIDGES),
                        irInt(requiredBridges.size)
                    ) + bridgeMethodTypes
                )
            } else {
                jvmInvokeDynamic(
                    dynamicCall,
                    jdkMetafactoryHandle,
                    listOf(samMethodType, irRawFunRef, instanceMethodType)
                )
            }
        }
    }

    private fun getOverriddenMethodsRequiringBridges(
        instanceMethod: IrSimpleFunction,
        samMethod: IrSimpleFunction,
        extraOverriddenMethods: List<IrSimpleFunction>
    ): Collection<IrSimpleFunction> {
        val jvmInstanceMethod = context.methodSignatureMapper.mapAsmMethod(instanceMethod)
        val jvmSamMethod = context.methodSignatureMapper.mapAsmMethod(samMethod)

        val signatureToNonFakeOverride = LinkedHashMap<Method, IrSimpleFunction>()
        for (overridden in extraOverriddenMethods) {
            val jvmOverriddenMethod = context.methodSignatureMapper.mapAsmMethod(overridden)
            if (jvmOverriddenMethod != jvmInstanceMethod && jvmOverriddenMethod != jvmSamMethod) {
                signatureToNonFakeOverride[jvmOverriddenMethod] = overridden
            }
        }
        return signatureToNonFakeOverride.values
    }

    private fun wrapClosureInDynamicCall(
        erasedSamType: IrSimpleType,
        samMethod: IrSimpleFunction,
        targetRef: IrFunctionReference
    ): IrCall {
        fun fail(message: String): Nothing =
            throw AssertionError("$message, irFunRef:\n${targetRef.dump()}")

        val dynamicCallArguments = ArrayList<IrExpression>()

        val irDynamicCallTarget = context.irFactory.buildFun {
            origin = JvmLoweredDeclarationOrigin.INVOVEDYNAMIC_CALL_TARGET
            name = samMethod.name
            returnType = erasedSamType
        }.apply {
            parent = context.ir.symbols.kotlinJvmInternalInvokeDynamicPackage

            val targetFun = targetRef.symbol.owner
            val refDispatchReceiver = targetRef.dispatchReceiver
            val refExtensionReceiver = targetRef.extensionReceiver

            var syntheticParameterIndex = 0
            var argumentStart = 0
            when (targetFun) {
                is IrSimpleFunction -> {
                    if (refDispatchReceiver != null) {
                        addValueParameter("p${syntheticParameterIndex++}", targetFun.dispatchReceiverParameter!!.type)
                        dynamicCallArguments.add(refDispatchReceiver)
                    }
                    if (refExtensionReceiver != null) {
                        addValueParameter("p${syntheticParameterIndex++}", targetFun.extensionReceiverParameter!!.type)
                        dynamicCallArguments.add(refExtensionReceiver)
                    }
                }
                is IrConstructor -> {
                    // At this point, outer class instances in inner class constructors are represented as regular value parameters.
                    // However, in a function reference to such constructors, bound receiver value is stored as a dispatch receiver.
                    if (refDispatchReceiver != null) {
                        addValueParameter("p${syntheticParameterIndex++}", targetFun.valueParameters[0].type)
                        dynamicCallArguments.add(refDispatchReceiver)
                        argumentStart++
                    }
                }
                else -> {
                    throw AssertionError("Unexpected function: ${targetFun.render()}")
                }
            }

            val samMethodValueParametersCount = samMethod.valueParameters.size +
                    if (samMethod.extensionReceiverParameter != null && refExtensionReceiver == null) 1 else 0
            val targetFunValueParametersCount = targetFun.valueParameters.size
            for (i in argumentStart until targetFunValueParametersCount - samMethodValueParametersCount) {
                val targetFunValueParameter = targetFun.valueParameters[i]
                addValueParameter("p${syntheticParameterIndex++}", targetFunValueParameter.type)
                val capturedValueArgument = targetRef.getValueArgument(i)
                    ?: fail("Captured value argument #$i (${targetFunValueParameter.render()}) not provided")
                dynamicCallArguments.add(capturedValueArgument)
            }
        }

        if (dynamicCallArguments.size != irDynamicCallTarget.valueParameters.size) {
            throw AssertionError(
                "Dynamic call target value parameters (${irDynamicCallTarget.valueParameters.size}) " +
                        "don't match dynamic call arguments (${dynamicCallArguments.size}):\n" +
                        "irDynamicCallTarget:\n" +
                        irDynamicCallTarget.dump() +
                        "dynamicCallArguments:\n" +
                        dynamicCallArguments
                            .withIndex()
                            .joinToString(separator = "\n ", prefix = "[\n ", postfix = "\n]") { (index, irArg) ->
                                "#$index: ${irArg.dump()}"
                            }
            )
        }

        return context.createJvmIrBuilder(irDynamicCallTarget.symbol)
            .irCall(irDynamicCallTarget.symbol)
            .apply {
                for (i in dynamicCallArguments.indices) {
                    putValueArgument(i, dynamicCallArguments[i])
                }
            }
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression = with(builder) {
        at(expression)
        return when (expression.operator) {
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT ->
                irComposite(resultType = expression.type) {
                    +expression.argument.transformVoid()
                    // TODO: Don't generate these casts in the first place
                    if (!expression.argument.type.isSubtypeOf(expression.type.makeNullable(), context.irBuiltIns)) {
                        +IrCompositeImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, expression.type)
                    }
                }

            // There is no difference between IMPLICIT_CAST and IMPLICIT_INTEGER_COERCION on JVM_IR.
            // Instead, this is handled in StackValue.coerce.
            IrTypeOperator.IMPLICIT_INTEGER_COERCION ->
                irImplicitCast(expression.argument.transformVoid(), expression.typeOperand)

            IrTypeOperator.CAST ->
                lowerCast(expression.argument.transformVoid(), expression.typeOperand)

            IrTypeOperator.SAFE_CAST ->
                if (expression.typeOperand.isReifiedTypeParameter) {
                    expression.transformChildrenVoid()
                    expression
                } else {
                    irLetS(
                        expression.argument.transformVoid(),
                        IrStatementOrigin.SAFE_CALL,
                        irType = context.irBuiltIns.anyNType
                    ) { valueSymbol ->
                        val thenPart =
                            if (valueSymbol.owner.type.isInlined())
                                lowerCast(irGet(valueSymbol.owner), expression.typeOperand)
                            else
                                irGet(valueSymbol.owner)
                        irIfThenElse(
                            expression.type,
                            lowerInstanceOf(irGet(valueSymbol.owner), expression.typeOperand.makeNotNull()),
                            thenPart,
                            irNull(expression.type)
                        )
                    }
                }

            IrTypeOperator.INSTANCEOF ->
                lowerInstanceOf(expression.argument.transformVoid(), expression.typeOperand)

            IrTypeOperator.NOT_INSTANCEOF ->
                irNot(lowerInstanceOf(expression.argument.transformVoid(), expression.typeOperand))

            IrTypeOperator.IMPLICIT_NOTNULL -> {
                val owner = scope.scopeOwnerSymbol.owner
                val source = if (owner is IrFunction && owner.isDelegated()) {
                    "${owner.name.asString()}(...)"
                } else {
                    val (startOffset, endOffset) = expression.extents()
                    sourceViewFor(parent as IrDeclaration).subSequence(startOffset, endOffset).toString()
                }

                irLetS(expression.argument.transformVoid(), irType = context.irBuiltIns.anyNType) { valueSymbol ->
                    irComposite(resultType = expression.type) {
                        +irCall(checkExpressionValueIsNotNull).apply {
                            putValueArgument(0, irGet(valueSymbol.owner))
                            putValueArgument(1, irString(source))
                        }
                        +irGet(valueSymbol.owner)
                    }
                }
            }

            else -> {
                expression.transformChildrenVoid()
                expression
            }
        }
    }

    private fun IrFunction.isDelegated() =
        origin == IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR ||
                origin == IrDeclarationOrigin.DELEGATED_MEMBER

    private fun IrElement.extents(): Pair<Int, Int> {
        var startOffset = UNDEFINED_OFFSET
        var endOffset = UNDEFINED_OFFSET
        acceptVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
                if (startOffset == UNDEFINED_OFFSET || element.startOffset != UNDEFINED_OFFSET && element.startOffset < startOffset)
                    startOffset = element.startOffset
                if (endOffset == UNDEFINED_OFFSET || element.endOffset != UNDEFINED_OFFSET && endOffset < element.endOffset)
                    endOffset = element.endOffset
            }
        })
        return startOffset to endOffset
    }

    private fun sourceViewFor(declaration: IrDeclaration): CharSequence =
        declaration.fileParent.getKtFile()!!.viewProvider.contents

    private val throwTypeCastException: IrSimpleFunctionSymbol =
        if (context.state.unifiedNullChecks)
            context.ir.symbols.throwNullPointerException
        else
            context.ir.symbols.throwTypeCastException

    private val checkExpressionValueIsNotNull: IrSimpleFunctionSymbol =
        if (context.state.unifiedNullChecks)
            context.ir.symbols.checkNotNullExpressionValue
        else
            context.ir.symbols.checkExpressionValueIsNotNull
}

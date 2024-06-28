/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.indy

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.JvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.getSingleAbstractMethod
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildValueParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions

internal sealed class SamDelegatingLambdaBlock {
    abstract val block: IrContainerExpression
    abstract val ref: IrFunctionReference
    abstract fun replaceRefWith(expression: IrExpression)
}


internal class RegularDelegatingLambdaBlock(
    override val block: IrContainerExpression,
    override val ref: IrFunctionReference
) : SamDelegatingLambdaBlock() {

    override fun replaceRefWith(expression: IrExpression) {
        block.statements[block.statements.size - 1] = expression
        block.type = expression.type
    }
}


internal class NullableDelegatingLambdaBlock(
    override val block: IrContainerExpression,
    override val ref: IrFunctionReference,
    private val ifExpr: IrExpression,
    private val ifNotNullBlock: IrContainerExpression
) : SamDelegatingLambdaBlock() {

    override fun replaceRefWith(expression: IrExpression) {
        ifNotNullBlock.statements[ifNotNullBlock.statements.size - 1] = expression
        ifNotNullBlock.type = expression.type
        ifExpr.type = expression.type
        block.type = expression.type
    }
}


internal class SamDelegatingLambdaBuilder(private val jvmContext: JvmBackendContext) {
    fun build(expression: IrExpression, superType: IrType, scopeSymbol: IrSymbol, parent: IrDeclarationParent): SamDelegatingLambdaBlock {
        return if (superType.isNullable() && expression.type.isNullable())
            buildNullableDelegatingLambda(expression, superType, scopeSymbol, parent)
        else
            buildRegularDelegatingLambda(expression, superType, scopeSymbol, parent)
    }

    private fun buildRegularDelegatingLambda(
        expression: IrExpression,
        superType: IrType,
        scopeSymbol: IrSymbol,
        parent: IrDeclarationParent
    ): SamDelegatingLambdaBlock {
        lateinit var ref: IrFunctionReference
        val block = jvmContext.createJvmIrBuilder(scopeSymbol, expression).run {
            //  {
            //      val tmp = <expression>
            //      fun `<anonymous>`(p1: T1, ..., pN: TN): R =
            //          tmp.invoke(p1, ..., pN)
            //      ::`<anonymous>`
            //  }

            irBlock(origin = IrStatementOrigin.LAMBDA) {
                val tmp = irTemporary(expression)
                val lambda = createDelegatingLambda(expression, superType, tmp, parent)
                    .also { +it }
                ref = createDelegatingLambdaReference(expression, lambda)
                    .also { +it }
            }
        }
        block.type = expression.type
        return RegularDelegatingLambdaBlock(block, ref)
    }

    private fun buildNullableDelegatingLambda(
        expression: IrExpression,
        superType: IrType,
        scopeSymbol: IrSymbol,
        parent: IrDeclarationParent
    ): SamDelegatingLambdaBlock {
        lateinit var ref: IrFunctionReference
        lateinit var ifExpr: IrExpression
        lateinit var ifNotNullBlock: IrContainerExpression
        val block = jvmContext.createJvmIrBuilder(scopeSymbol, expression).run {
            //  {
            //      val tmp = <expression>
            //      if (tmp == null)
            //          null
            //      else {
            //          fun `<anonymous>`(p1: T1, ..., pN: TN): R =
            //              tmp.invoke(p1, ..., pN)
            //          ::`<anonymous>`
            //      }
            //  }

            irBlock(origin = IrStatementOrigin.LAMBDA) {
                val tmp = irTemporary(expression)
                ifNotNullBlock = irBlock {
                    val lambda = createDelegatingLambda(expression, superType, tmp, parent)
                        .also { +it }
                    ref = createDelegatingLambdaReference(expression, lambda)
                        .also { +it }
                }
                ifExpr = irIfNull(expression.type, irGet(tmp), irNull(), ifNotNullBlock)
                    .also { +it }
            }
        }
        block.type = expression.type
        return NullableDelegatingLambdaBlock(block, ref, ifExpr, ifNotNullBlock)
    }

    private fun createDelegatingLambda(
        expression: IrExpression,
        superType: IrType,
        tmp: IrVariable,
        parent: IrDeclarationParent
    ): IrSimpleFunction {
        val superMethod = superType.getClass()?.getSingleAbstractMethod()
            ?: throw AssertionError("SAM type expected: ${superType.render()}")
        val effectiveValueParametersCount = superMethod.valueParameters.size +
                if (superMethod.extensionReceiverParameter == null) 0 else 1
        val invocableFunctionClass =
            if (superMethod.isSuspend)
                jvmContext.ir.symbols.suspendFunctionN(effectiveValueParametersCount).owner
            else
                jvmContext.ir.symbols.functionN(effectiveValueParametersCount).owner
        val invokeFunction = invocableFunctionClass.functions.single { it.name == OperatorNameConventions.INVOKE }
        val typeSubstitutor = createTypeSubstitutor(superType)

        return jvmContext.irFactory.buildFun {
            name = Name.special("<anonymous>")
            returnType = typeSubstitutor.substitute(superMethod.returnType)
            visibility = DescriptorVisibilities.LOCAL
            modality = Modality.FINAL
            origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
            isSuspend = superMethod.isSuspend
        }.also { lambda ->
            lambda.dispatchReceiverParameter = null
            lambda.extensionReceiverParameter = null
            lambda.valueParameters = createLambdaValueParameters(superMethod, lambda, typeSubstitutor)
            lambda.body = jvmContext.createJvmIrBuilder(lambda.symbol, expression)
                .irBlockBody {
                    +irReturn(
                        irCall(invokeFunction).also { invokeCall ->
                            // We need to cast receiver to the function type because it might have an imaginary type like KFunction2 which
                            // is mapped to KFunction in the codegen by default, which has no 'invoke'. Looks like correct type arguments
                            // are not needed here, so we use "raw" type for simplicity. If that stops working, we'll need to compute the
                            // correct substitution of invocableFunctionClass by visiting tmp.type's hierarchy.
                            val rawFunctionType = invocableFunctionClass.typeWith()

                            invokeCall.dispatchReceiver = irImplicitCast(irGet(tmp), rawFunctionType)
                            var parameterIndex = 0
                            invokeFunction.extensionReceiverParameter?.let {
                                invokeCall.extensionReceiver = irGet(lambda.valueParameters[parameterIndex++])
                            }
                            for (argumentIndex in invokeFunction.valueParameters.indices) {
                                invokeCall.putValueArgument(argumentIndex, irGet(lambda.valueParameters[parameterIndex++]))
                            }
                        }
                    )
                }
            lambda.parent = parent
        }
    }

    private fun createLambdaValueParameters(
        superMethod: IrSimpleFunction,
        lambda: IrSimpleFunction,
        typeSubstitutor: IrTypeSubstitutor
    ): List<IrValueParameter> {
        val lambdaParameters = ArrayList<IrValueParameter>()
        var index = 0
        superMethod.extensionReceiverParameter?.let { superExtensionReceiver ->
            lambdaParameters.add(superExtensionReceiver.copySubstituted(lambda, typeSubstitutor, index++, Name.identifier("\$receiver")))
        }
        superMethod.valueParameters.mapTo(lambdaParameters) { superValueParameter ->
            superValueParameter.copySubstituted(lambda, typeSubstitutor, index++)
        }
        return lambdaParameters
    }

    private fun IrValueParameter.copySubstituted(
        function: IrSimpleFunction,
        substitutor: IrTypeSubstitutor,
        newIndex: Int,
        newName: Name = name
    ) =
        buildValueParameter(function) {
            name = newName
            index = newIndex
            type = substitutor.substitute(this@copySubstituted.type)
        }

    private fun JvmIrBuilder.createDelegatingLambdaReference(expression: IrExpression, lambda: IrSimpleFunction): IrFunctionReference {
        return IrFunctionReferenceImpl(
            startOffset, endOffset,
            expression.type,
            lambda.symbol,
            typeArgumentsCount = 0,
            valueArgumentsCount = lambda.valueParameters.size,
            reflectionTarget = null,
            origin = IrStatementOrigin.LAMBDA
        )
    }

    private fun createTypeSubstitutor(irType: IrType): IrTypeSubstitutor {
        if (irType !is IrSimpleType)
            throw AssertionError("Simple type expected: ${irType.render()}")
        val irClassSymbol = irType.classOrNull
            ?: throw AssertionError("Class type expected: ${irType.render()}")
        return IrTypeSubstitutor(irClassSymbol.owner.typeParameters.map { it.symbol }, irType.arguments)
    }
}

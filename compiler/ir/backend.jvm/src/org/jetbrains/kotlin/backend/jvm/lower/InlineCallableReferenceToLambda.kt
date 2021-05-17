/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.IrInlineReferenceLocator
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.irArray
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addExtensionReceiver
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

internal val inlineCallableReferenceToLambdaPhase = makeIrFilePhase(
    ::InlineCallableReferenceToLambdaPhase,
    name = "InlineCallableReferenceToLambdaPhase",
    description = "Transform callable reference to inline lambda"
)

// This lowering transforms CR passed to inline function to lambda which would be inlined
//
//      inline fun foo(inlineParameter: (A) -> B): B {
//          return inlineParameter()
//      }
//
//      foo(::smth) -> foo { a -> smth(a) }
//
internal class InlineCallableReferenceToLambdaPhase(val context: JvmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        val inlinableReferences = mutableSetOf<IrCallableReference<*>>()
        irFile.accept(object : IrInlineReferenceLocator(context) {
            override fun visitInlineReference(argument: IrCallableReference<*>) {
                inlinableReferences.add(argument)
            }

            override fun visitInlineLambda(
                argument: IrFunctionReference, callee: IrFunction, parameter: IrValueParameter, scope: IrDeclaration
            ) {
                // Obviously needs no extra wrapping.
            }
        }, null)
        irFile.transformChildrenVoid(InlineCallableReferenceToLambdaTransformer(context, inlinableReferences))
    }
}

const val STUB_FOR_INLINING = "stub_for_inlining"

private class InlineCallableReferenceToLambdaTransformer(
    val context: JvmBackendContext,
    val inlinableReferences: Set<IrCallableReference<*>>
) : IrElementTransformerVoidWithContext() {
    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
        expression.transformChildrenVoid(this)
        if (expression !in inlinableReferences) return expression
        return expandInlineFunctionReferenceToLambda(expression, expression.symbol.owner)
    }

    override fun visitPropertyReference(expression: IrPropertyReference): IrExpression {
        expression.transformChildrenVoid(this)
        if (expression !in inlinableReferences) return expression

        return if (expression.field?.owner == null) {
            // Use getter if field is absent ...
            expandInlineFunctionReferenceToLambda(expression, expression.getter!!.owner)
        } else {
            // ... else use field itself
            expandInlineFieldReferenceToLambda(expression, expression.field!!.owner)
        }
    }

    private fun expandInlineFieldReferenceToLambda(expression: IrPropertyReference, field: IrField): IrExpression {
        val irBuilder = context.createJvmIrBuilder(currentScope!!.scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset)
        return irBuilder.irBlock(expression, IrStatementOrigin.LAMBDA) {
            val boundReceiver = expression.dispatchReceiver ?: expression.extensionReceiver
            val function = context.irFactory.buildFun {
                setSourceRange(expression)
                origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
                name = Name.identifier("stub_for_inline")
                visibility = DescriptorVisibilities.LOCAL
                returnType = field.type
                isSuspend = false
            }.apply {
                parent = currentDeclarationParent ?: error("No current declaration parent at ${expression.dump()}")
                val receiver = when {
                    field.isStatic -> null
                    boundReceiver != null -> irGet(addExtensionReceiver(boundReceiver.type))
                    else -> irGet(addValueParameter("receiver", field.parentAsClass.defaultType))
                }
                body = this@InlineCallableReferenceToLambdaTransformer.context.createIrBuilder(symbol).run {
                    irExprBody(irGetField(receiver, field))
                }
            }

            +function
            +IrFunctionReferenceImpl.fromSymbolOwner(
                expression.startOffset,
                expression.endOffset,
                expression.type,
                function.symbol,
                typeArgumentsCount = 0,
                reflectionTarget = null,
                origin = IrStatementOrigin.LAMBDA
            ).apply {
                copyAttributes(expression)
                extensionReceiver = boundReceiver
            }
        }
    }

    private class BoundReceiver(
        val receiverParameter: IrValueParameter,
        val receiverValue: IrExpression,
        val receiverType: IrType
    )

    private fun IrCallableReference<*>.getBoundReceiver(): BoundReceiver? {
        val irFunction = when (this) {
            is IrFunctionReference ->
                this.symbol.owner
            is IrPropertyReference ->
                this.getter!!.owner
            else ->
                throw AssertionError("Unexpected callable reference: ${this.render()}")
        }

        this.dispatchReceiver?.let { dispatchReceiver ->
            val dispatchReceiverParameter = irFunction.dispatchReceiverParameter
                ?: throw AssertionError("Referenced declaration '${this.symbol.owner.render()}' has no dispatch receiver: ${this.dump()}")
            // NB in case of fake override of Base::foo in Child::foo, dispatch receiver type is Base,
            // but we in fact need Child (because of accessor generation).
            return BoundReceiver(dispatchReceiverParameter, dispatchReceiver, irFunction.parentAsClass.defaultType)
        }

        this.extensionReceiver?.let { extensionReceiver ->
            val extensionReceiverParameter = irFunction.extensionReceiverParameter
                ?: throw AssertionError("Referenced declaration '${this.symbol.owner.render()}' has no extension receiver: ${this.dump()}")
            return BoundReceiver(extensionReceiverParameter, extensionReceiver, extensionReceiverParameter.type)
        }

        return null
    }

    private fun expandInlineFunctionReferenceToLambda(expression: IrCallableReference<*>, referencedFunction: IrFunction): IrExpression {
        val irBuilder = context.createJvmIrBuilder(currentScope!!.scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset)
        return irBuilder.irBlock(expression, IrStatementOrigin.LAMBDA) {
            val boundReceiver = expression.getBoundReceiver()
            val argumentTypes = (expression.type as IrSimpleType).arguments.dropLast(1).map { (it as IrTypeProjection).type }

            val function = context.irFactory.buildFun {
                setSourceRange(expression)
                origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
                name = Name.identifier(STUB_FOR_INLINING)
                visibility = DescriptorVisibilities.LOCAL
                returnType = referencedFunction.returnType
                isSuspend = referencedFunction.isSuspend
            }.apply {
                parent = currentDeclarationParent!!
                if (boundReceiver != null) {
                    addExtensionReceiver(boundReceiver.receiverType)
                }
                for ((index, argumentType) in argumentTypes.withIndex()) {
                    addValueParameter {
                        name = Name.identifier("p$index")
                        type = argumentType
                    }
                }

                body = this@InlineCallableReferenceToLambdaTransformer.context.createJvmIrBuilder(
                    symbol,
                    expression.startOffset,
                    expression.endOffset
                ).run {
                    irExprBody(irCall(referencedFunction).apply {
                        symbol.owner.allTypeParameters.forEach {
                            putTypeArgument(it.index, expression.getTypeArgument(it.index))
                        }

                        var unboundIndex = 0
                        for (parameter in referencedFunction.explicitParameters) {
                            when {
                                boundReceiver?.receiverParameter == parameter ->
                                    irGet(extensionReceiverParameter!!)
                                parameter.isVararg && unboundIndex < argumentTypes.size && parameter.type == valueParameters[unboundIndex].type ->
                                    irGet(valueParameters[unboundIndex++])
                                parameter.isVararg && (unboundIndex < argumentTypes.size || !parameter.hasDefaultValue()) ->
                                    irArray(parameter.type) {
                                        (unboundIndex until argumentTypes.size).forEach { +irGet(valueParameters[unboundIndex++]) }
                                    }
                                unboundIndex >= argumentTypes.size ->
                                    null
                                else ->
                                    irGet(valueParameters[unboundIndex++])
                            }?.let { putArgument(referencedFunction, parameter, it) }
                        }
                    })
                }
            }

            +function
            +IrFunctionReferenceImpl.fromSymbolOwner(
                expression.startOffset,
                expression.endOffset,
                expression.type,
                function.symbol,
                typeArgumentsCount = function.typeParameters.size,
                reflectionTarget = null,
                origin = IrStatementOrigin.LAMBDA
            ).apply {
                copyAttributes(expression)
                extensionReceiver = boundReceiver?.receiverValue
            }
        }
    }
}

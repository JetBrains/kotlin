/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.allParameters
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.IrInlineReferenceLocator
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.irArray
import org.jetbrains.kotlin.codegen.AsmUtil.BOUND_REFERENCE_RECEIVER
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
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
            val function = context.irFactory.buildFun {
                setSourceRange(expression)
                origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
                name = Name.identifier("stub_for_inline")
                visibility = DescriptorVisibilities.LOCAL
                returnType = field.type
                isSuspend = false
            }.apply {
                parent = currentDeclarationParent ?: error("No current declaration parent at ${expression.dump()}")
                val boundReceiver = expression.dispatchReceiver ?: expression.extensionReceiver

                val receiver =
                    when {
                        field.isStatic -> null
                        boundReceiver != null -> irGet(irTemporary(boundReceiver, BOUND_REFERENCE_RECEIVER))
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
                field.type,
                function.symbol,
                typeArgumentsCount = 0,
                reflectionTarget = null,
                origin = IrStatementOrigin.LAMBDA
            ).apply {
                copyAttributes(expression)
            }
        }
    }

    private fun expandInlineFunctionReferenceToLambda(expression: IrCallableReference<*>, referencedFunction: IrFunction): IrExpression {
        val irBuilder = context.createJvmIrBuilder(currentScope!!.scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset)
        return irBuilder.irBlock(expression, IrStatementOrigin.LAMBDA) {
            // We find the number of parameters for constructed lambda from the type of the function reference,
            // but the actual types have to be copied from referencedFunction; function reference argument type may be too
            // specific because of approximation. See compiler/testData/codegen/box/callableReference/function/argumentTypes.kt
            val boundReceiver: Pair<IrValueParameter, IrExpression>? = expression.getArgumentsWithIr().singleOrNull()
            val nParams = (expression.type as IrSimpleType).arguments.size - 1
            var toDropAtStart = 0
            if (boundReceiver != null) toDropAtStart++
            if (referencedFunction is IrConstructor) toDropAtStart++
            val argumentTypes = referencedFunction.allParameters.drop(toDropAtStart).take(nParams).map { parameter ->
                parameter.type.substitute(
                    referencedFunction.typeParameters,
                    referencedFunction.typeParameters.indices.map { expression.getTypeArgument(it)!! }
                )
            }

            val function = context.irFactory.buildFun {
                setSourceRange(expression)
                origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
                name = Name.identifier("stub_for_inlining")
                visibility = DescriptorVisibilities.LOCAL
                returnType = referencedFunction.returnType
                isSuspend = referencedFunction.isSuspend
            }.apply {
                parent = currentDeclarationParent!!
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
                                boundReceiver?.first == parameter ->
                                    irGet(irTemporary(boundReceiver.second))
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
                function.returnType,
                function.symbol,
                typeArgumentsCount = function.typeParameters.size,
                reflectionTarget = null,
                origin = IrStatementOrigin.LAMBDA
            ).apply {
                copyAttributes(expression)
            }
        }
    }
}

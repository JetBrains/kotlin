/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.ir.returnType
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildVariable
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.deepCopyWithVariables
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrReturnableBlockSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.setDeclarationsParent
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

object FINALLY_EXPRESSION : IrStatementOriginImpl("FINALLY_EXPRESSION")

class FinallyBlocksLowering(val context: CommonBackendContext, private val throwableType: IrType): FileLoweringPass, IrElementTransformerVoidWithContext() {

    private interface HighLevelJump {
        fun toIr(context: CommonBackendContext, startOffset: Int, endOffset: Int, value: IrExpression): IrExpression
    }

    private data class Return(val target: IrReturnTargetSymbol): HighLevelJump {
        override fun toIr(context: CommonBackendContext, startOffset: Int, endOffset: Int, value: IrExpression)
                =
            IrReturnImpl(startOffset, endOffset, context.irBuiltIns.nothingType, target, value)
    }

    private data class Break(val loop: IrLoop): HighLevelJump {
        override fun toIr(context: CommonBackendContext, startOffset: Int, endOffset: Int, value: IrExpression)
                = IrCompositeImpl(
            startOffset, endOffset, context.irBuiltIns.unitType, null,
            statements = listOf(
                value,
                IrBreakImpl(startOffset, endOffset, context.irBuiltIns.nothingType, loop)
            )
        )
    }

    private data class Continue(val loop: IrLoop): HighLevelJump {
        override fun toIr(context: CommonBackendContext, startOffset: Int, endOffset: Int, value: IrExpression)
                = IrCompositeImpl(
            startOffset, endOffset, context.irBuiltIns.unitType, null,
            statements = listOf(
                value,
                IrContinueImpl(startOffset, endOffset, context.irBuiltIns.nothingType, loop)
            )
        )
    }

    private abstract class Scope

    private class ReturnableScope(val symbol: IrReturnTargetSymbol) : Scope()

    private class LoopScope(val loop: IrLoop): Scope()

    private class TryScope(var expression: IrExpression,
                           val finallyExpression: IrExpression,
                           val irBuilder: IrBuilderWithScope
    ): Scope() {
        val jumps = mutableMapOf<HighLevelJump, IrReturnTargetSymbol>()
    }

    private val otherScopeStack = mutableListOf<Scope>()

    private inline fun <S: Scope, R> using(scope: S, block: (S) -> R): R {
        otherScopeStack.push(scope)
        try {
            return block(scope)
        } finally {
            otherScopeStack.pop()
        }
    }

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    override fun visitFunctionNew(declaration: IrFunction): IrStatement {
        using(ReturnableScope(declaration.symbol)) {
            return super.visitFunctionNew(declaration)
        }
    }

    override fun visitContainerExpression(expression: IrContainerExpression): IrExpression {
        if (expression !is IrReturnableBlockImpl)
            return super.visitContainerExpression(expression)

        using(ReturnableScope(expression.symbol)) {
            return super.visitContainerExpression(expression)
        }
    }

    override fun visitLoop(loop: IrLoop): IrExpression {
        using(LoopScope(loop)) {
            return super.visitLoop(loop)
        }
    }

    override fun visitBreak(jump: IrBreak): IrExpression {
        val startOffset = jump.startOffset
        val endOffset = jump.endOffset
        val irBuilder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, startOffset, endOffset)
        return performHighLevelJump(
            targetScopePredicate = { it is LoopScope && it.loop == jump.loop },
            jump                 = Break(jump.loop),
            startOffset          = startOffset,
            endOffset            = endOffset,
            value                = irBuilder.irGetObject(context.irBuiltIns.unitClass)
        ) ?: jump
    }

    override fun visitContinue(jump: IrContinue): IrExpression {
        val startOffset = jump.startOffset
        val endOffset = jump.endOffset
        val irBuilder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, startOffset, endOffset)
        return performHighLevelJump(
            targetScopePredicate = { it is LoopScope && it.loop == jump.loop },
            jump                 = Continue(jump.loop),
            startOffset          = startOffset,
            endOffset            = endOffset,
            value                = irBuilder.irGetObject(context.irBuiltIns.unitClass)
        ) ?: jump
    }

    override fun visitReturn(expression: IrReturn): IrExpression {
        expression.transformChildrenVoid(this)

        return performHighLevelJump(
            targetScopePredicate = { it is ReturnableScope && it.symbol == expression.returnTargetSymbol },
            jump                 = Return(expression.returnTargetSymbol),
            startOffset          = expression.startOffset,
            endOffset            = expression.endOffset,
            value                = expression.value
        ) ?: expression
    }

    private fun performHighLevelJump(targetScopePredicate: (Scope) -> Boolean,
                                     jump: HighLevelJump,
                                     startOffset: Int,
                                     endOffset: Int,
                                     value: IrExpression
    ): IrExpression? {
        val tryScopes = otherScopeStack.reversed()
                .takeWhile { !targetScopePredicate(it) }
                .filterIsInstance<TryScope>()
                .toList()
        if (tryScopes.isEmpty())
            return null
        return performHighLevelJump(tryScopes, 0, jump, startOffset, endOffset, value)
    }

    private fun performHighLevelJump(tryScopes: List<TryScope>,
                                     index: Int,
                                     jump: HighLevelJump,
                                     startOffset: Int,
                                     endOffset: Int,
                                     value: IrExpression
    ): IrExpression {
        if (index == tryScopes.size)
            return jump.toIr(context, startOffset, endOffset, value)

        val currentTryScope = tryScopes[index]
        currentTryScope.jumps.getOrPut(jump) {
            val type = (jump as? Return)?.target?.owner?.returnType(context) ?: value.type
            jump.toString()
            val symbol = IrReturnableBlockSymbolImpl()
            with(currentTryScope) {
                irBuilder.run {
                    val inlinedFinally = irInlineFinally(symbol, type, expression, finallyExpression)
                    expression = performHighLevelJump(
                            tryScopes   = tryScopes,
                            index       = index + 1,
                            jump        = jump,
                            startOffset = startOffset,
                            endOffset   = endOffset,
                            value       = inlinedFinally)
                }
            }
            symbol
        }.let {
            return IrReturnImpl(
                startOffset = startOffset,
                endOffset = endOffset,
                type = context.irBuiltIns.nothingType,
                returnTargetSymbol = it,
                value = value
            )
        }
    }

    override fun visitTry(aTry: IrTry): IrExpression {
        val finallyExpression = aTry.finallyExpression
                ?: return super.visitTry(aTry)

        val startOffset = aTry.startOffset
        val endOffset = aTry.endOffset
        val irBuilder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, startOffset, endOffset)
        val transformer = this
        irBuilder.run {
            val transformedFinallyExpression = finallyExpression.transform(transformer, null)
            val catchParameter = buildVariable(
                scope.getLocalDeclarationParent(), startOffset, endOffset, IrDeclarationOrigin.CATCH_PARAMETER, Name.identifier("t"),
                throwableType
            )

            val syntheticTry = IrTryImpl(
                startOffset = startOffset,
                endOffset = endOffset,
                type = context.irBuiltIns.nothingType
            ).apply {
                this.catches += irCatch(
                    catchParameter,
                    irComposite {
                        +copy(finallyExpression)
                        +irThrow(irGet(catchParameter))
                    }
                )

                this.finallyExpression = null
            }

            using(TryScope(syntheticTry, transformedFinallyExpression, this)) {

                val fallThroughType = aTry.type
                val fallThroughSymbol = IrReturnableBlockSymbolImpl()
                val transformedResult = aTry.tryResult.transform(transformer, null)
                val returnedResult = irReturn(fallThroughSymbol, transformedResult)

                if (aTry.catches.isNotEmpty()) {
                    val transformedTry = IrTryImpl(
                        startOffset = startOffset,
                        endOffset = endOffset,
                        type = context.irBuiltIns.nothingType
                    )
                    transformedTry.tryResult = returnedResult
                    for (aCatch in aTry.catches) {
                        val transformedCatch = aCatch.transform(transformer, null)
                        transformedCatch.result = irReturn(fallThroughSymbol, transformedCatch.result)
                        transformedTry.catches.add(transformedCatch)
                    }
                    syntheticTry.tryResult = transformedTry
                } else {
                    syntheticTry.tryResult = returnedResult
                }

                return irInlineFinally(fallThroughSymbol, fallThroughType, it.expression, it.finallyExpression)
            }
        }
    }

    private fun IrBuilderWithScope.irInlineFinally(symbol: IrReturnableBlockSymbol, type: IrType,
                                                                                    value: IrExpression,
                                                                                    finallyExpression: IrExpression
    ): IrExpression {
        return when {
            type.isUnit() || type.isNothing() -> irBlock(value, null, type) {
                +irReturnableBlock(symbol, type) {
                    +value
                }
                +irComposite(resultType = context.irBuiltIns.unitType, origin = FINALLY_EXPRESSION) {
                    +copy(finallyExpression)
                }
            }
            else -> irBlock(value, null, type) {
                val tmp = createTmpVariable(irReturnableBlock(symbol, type) {
                    +irReturn(symbol, value)
                })
                +irComposite(resultType = context.irBuiltIns.unitType, origin = FINALLY_EXPRESSION) {
                    +copy(finallyExpression)
                }
                +irGet(tmp)
            }
        }
    }

    private inline fun <reified T : IrElement> IrBuilderWithScope.copy(element: T) =
        element.deepCopyWithVariables().setDeclarationsParent(parent)

    fun IrBuilderWithScope.irReturn(target: IrReturnTargetSymbol, value: IrExpression) =
        IrReturnImpl(startOffset, endOffset, context.irBuiltIns.nothingType, target, value)

    private inline fun IrBuilderWithScope.irReturnableBlock(symbol: IrReturnableBlockSymbol, type: IrType, body: IrBlockBuilder.() -> Unit) =
        IrReturnableBlockImpl(
            startOffset, endOffset, type, symbol, null,
            IrBlockBuilder(context, scope, startOffset, endOffset, null, type, true)
                .block(body).statements
        )
}
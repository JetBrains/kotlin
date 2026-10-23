/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.ir.returnType
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildVariable
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrReturnableBlockSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

val FINALLY_EXPRESSION by IrStatementOriginImpl
val SYNTHETIC_CATCH_FOR_FINALLY_EXPRESSION by IrStatementOriginImpl

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

    override fun lower(irFile: IrFile) { TODO() }

    override fun visitFunctionNew(declaration: IrFunction): IrStatement { TODO() }

    override fun visitContainerExpression(expression: IrContainerExpression): IrExpression { TODO() }

    override fun visitLoop(loop: IrLoop): IrExpression { TODO() }

    override fun visitBreak(jump: IrBreak): IrExpression { TODO() }

    override fun visitContinue(jump: IrContinue): IrExpression { TODO() }

    override fun visitReturn(expression: IrReturn): IrExpression { TODO() }

    private fun performHighLevelJump(targetScopePredicate: (Scope) -> Boolean,
                                     jump: HighLevelJump,
                                     startOffset: Int,
                                     endOffset: Int,
                                     value: IrExpression
    ): IrExpression? { TODO() }

    private fun performHighLevelJump(tryScopes: List<TryScope>,
                                     index: Int,
                                     jump: HighLevelJump,
                                     startOffset: Int,
                                     endOffset: Int,
                                     value: IrExpression
    ): IrExpression { TODO() }

    override fun visitTry(aTry: IrTry): IrExpression { TODO() }

    private fun IrBuilderWithScope.irInlineFinally(symbol: IrReturnableBlockSymbol, type: IrType,
                                                                                    value: IrExpression,
                                                                                    finallyExpression: IrExpression
    ): IrExpression { TODO() }

    private inline fun <reified T : IrElement> IrBuilderWithScope.copy(element: T) =
        element.deepCopyWithSymbols(parent)

    fun IrBuilderWithScope.irReturn(target: IrReturnTargetSymbol, value: IrExpression) =
        IrReturnImpl(startOffset, endOffset, context.irBuiltIns.nothingType, target, value)

    private inline fun IrBuilderWithScope.irReturnableBlock(symbol: IrReturnableBlockSymbol, type: IrType, body: IrBlockBuilder.() -> Unit) =
        IrReturnableBlockImpl(
            startOffset, endOffset, type, symbol, null,
            IrBlockBuilder(context, scope, startOffset, endOffset, null, type, true)
                .block(body).statements
        )
}

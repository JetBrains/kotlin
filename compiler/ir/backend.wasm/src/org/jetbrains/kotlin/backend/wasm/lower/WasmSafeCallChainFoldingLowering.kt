/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.optimizations.ElvisInfo
import org.jetbrains.kotlin.backend.common.lower.optimizations.SafeCallInfo
import org.jetbrains.kotlin.backend.common.lower.optimizations.parseElvis
import org.jetbrains.kotlin.backend.common.lower.optimizations.parseSafeCall
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.impl.IrReturnableBlockSymbolImpl
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

private val FOLDED_SAFE_CALL by IrStatementOriginImpl

/**
 * Folds safe call chains to more compact forms and avoid extra boxing for primitives.
 */
internal class WasmSafeCallChainFoldingLowering(val context: WasmBackendContext) : FileLoweringPass {
    private val transformer = Transformer(context.irBuiltIns)

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(transformer)
    }
}

private class Transformer(private val builtIns: IrBuiltIns) : IrElementTransformerVoid() {
    override fun visitBlock(expression: IrBlock): IrExpression {
        expression.transformChildrenVoid()

        val safeCallInfo = expression.parseSafeCall(builtIns)
        if (safeCallInfo != null) {
            return foldSafeCall(safeCallInfo)
        }

        val elvisInfo = expression.parseElvis(builtIns)
        if (elvisInfo != null) {
            return foldElvis(elvisInfo)
        }

        return expression
    }

    /**
     * Transforms a safe call block (see [parseSafeCall]) into a returnable block with early `null` return.
     *
     * If the safe receiver is not a folded safe call (the first `?.` in the chain):
     * ```
     * {
     *     val tmp = <safe_receiver>
     *     when {
     *         tmp == null -> null
     *         else -> <safe_call_result>
     *     }
     * }
     * ```
     * is transformed to:
     * ```
     * FOLDED_SAFE_CALL@L {
     *     val tmp = <safe_receiver>
     *     when {
     *         tmp == null -> return@L null
     *     }
     *     return@L <safe_call_result>
     * }
     * ```
     *
     * If the safe receiver is an already folded safe call (the next `?.` in the chain):
     * ```
     * {
     *     val tmp = FOLDED_SAFE_CALL@L {
     *         <previous_statements>
     *         return@L <previous_result>
     *     }
     *     when {
     *         tmp == null -> null
     *         else -> <safe_call_result>
     *     }
     * }
     * ```
     * the existing returnable block is extended in-place instead of creating a nested one:
     * ```
     * FOLDED_SAFE_CALL@L {
     *     <previous_statements>
     *     val tmp = <previous_result> // the type of tmp becomes the type of <previous_result>
     *     when {                      // only if <previous_result> is nullable
     *         tmp == null -> return@L null
     *     }
     *     return@L <safe_call_result> // if <previous_result> is not nullable, smart casts of tmp are removed
     * }
     * ```
     */
    private fun foldSafeCall(safeCallInfo: SafeCallInfo): IrExpression {
        val safeCallBlock = safeCallInfo.block
        val startOffset = safeCallBlock.startOffset
        val endOffset = safeCallBlock.endOffset
        val safeCallType = safeCallBlock.type
        val safeCallTmpVal = safeCallInfo.tmpVal

        val tmpValInitializer = safeCallTmpVal.initializer
        if (tmpValInitializer is IrReturnableBlock && tmpValInitializer.origin == FOLDED_SAFE_CALL) {
            // this is a call of a()?.b()... where a() is a first expression in the chain
            val statements = tmpValInitializer.statements
            val lastResultValue = (statements.removeAt(statements.lastIndex) as IrReturn).value
            val lastResultValueType = lastResultValue.type

            safeCallTmpVal.initializer = lastResultValue
            safeCallTmpVal.type = lastResultValueType
            tmpValInitializer.type = safeCallType

            statements += safeCallTmpVal

            if (lastResultValueType.isNullable()) {
                // This is a call of ...a()?.b()... where a() is nullable
                statements += IrWhenImpl(startOffset, endOffset, builtIns.unitType).apply {
                    branches += IrBranchImpl(
                        safeCallInfo.ifNullBranch.condition,
                        IrReturnImpl(
                            startOffset = startOffset,
                            endOffset = endOffset,
                            type = builtIns.nothingType,
                            returnTargetSymbol = tmpValInitializer.symbol,
                            value = IrConstImpl.constNull(startOffset, endOffset, builtIns.nothingNType)
                        ),
                    )
                }
            } else {
                // This is a call of ...a()?.b()... where a() is not nullable
                val smartCastRemover = object : IrElementTransformerVoid() {
                    // Replace smartcast access to regular value get
                    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
                        if (expression.operator == IrTypeOperator.IMPLICIT_CAST) {
                            val argumentSymbol = expression.argument as? IrGetValue
                            if (argumentSymbol?.symbol == safeCallTmpVal.symbol) {
                                visitGetValue(argumentSymbol)
                                return expression.argument
                            }
                        }
                        return super.visitTypeOperator(expression)
                    }

                    // Fixup get value type according temporary value type
                    override fun visitGetValue(expression: IrGetValue): IrExpression {
                        if (expression.symbol == safeCallTmpVal.symbol) {
                            expression.type = safeCallTmpVal.type
                        }
                        return super.visitGetValue(expression)
                    }
                }
                smartCastRemover.visitElement(safeCallInfo.ifNotNullBranch.result)
            }

            statements += IrReturnImpl(
                startOffset = startOffset,
                endOffset = endOffset,
                type = builtIns.nothingType,
                returnTargetSymbol = tmpValInitializer.symbol,
                value = safeCallInfo.ifNotNullBranch.result
            )

            return tmpValInitializer
        } else {
            // this is a call of ...a()?.b()... where a() is a not first expression in the chain
            val symbol = IrReturnableBlockSymbolImpl()
            val newBlock = IrReturnableBlockImpl(startOffset, endOffset, safeCallType, symbol, FOLDED_SAFE_CALL).apply {
                statements += safeCallTmpVal
                statements += IrWhenImpl(startOffset, endOffset, builtIns.unitType).apply {
                    branches += IrBranchImpl(
                        safeCallInfo.ifNullBranch.condition,
                        IrReturnImpl(
                            startOffset, endOffset, safeCallType, symbol,
                            IrConstImpl.constNull(startOffset, endOffset, builtIns.nothingNType)
                        ),
                    )
                }

                statements += IrReturnImpl(
                    startOffset,
                    endOffset,
                    safeCallInfo.ifNotNullBranch.result.type,
                    symbol,
                    safeCallInfo.ifNotNullBranch.result
                )
            }
            return newBlock
        }
    }

    /**
     * Transforms an elvis block (see [parseElvis]) whose left-hand side is a folded safe call,
     * so that the right-hand side is evaluated only when the chain returns early with `null`.
     *
     * Applied only if the elvis type and the type of the last result of the chain are not nullable:
     * ```
     * {
     *     val tmp = FOLDED_SAFE_CALL@L1 {
     *         <statements> // contain `return@L1 null` for each `?.` of the chain
     *         return@L1 <chain_result>
     *     }
     *     when {
     *         tmp == null -> <elvis_rhs>
     *         else -> tmp
     *     }
     * }
     * ```
     * is transformed to:
     * ```
     * FOLDED_SAFE_CALL@L2 {
     *     FOLDED_SAFE_CALL@L1 {
     *         <statements> // `return@L1 null` exits only the inner block and falls through to <elvis_rhs>
     *         return@L2 <chain_result>
     *     }
     *     return@L2 <elvis_rhs>
     * }
     * ```
     * Otherwise, the elvis block is left unchanged.
     */
    private fun foldElvis(elvisInfo: ElvisInfo): IrExpression {
        val elvisLhs = elvisInfo.elvisLhs
        val elvisBlock = elvisInfo.block
        val startOffset = elvisBlock.startOffset
        val endOffset = elvisBlock.endOffset
        val elvisType = elvisBlock.type

        if (elvisLhs is IrReturnableBlock && elvisLhs.origin == FOLDED_SAFE_CALL && !elvisType.isNullable()) {
            // this is a call of a()?.b() ?: c()... where c() goes after a chain
            val lastReturn = elvisLhs.statements.last() as IrReturn
            if (lastReturn.value.type.isNullable()) return elvisBlock

            val symbol = IrReturnableBlockSymbolImpl()
            val newBlock = IrReturnableBlockImpl(startOffset, endOffset, elvisType, symbol, FOLDED_SAFE_CALL).apply {
                statements += elvisLhs
                lastReturn.returnTargetSymbol = symbol
                statements += IrReturnImpl(
                    startOffset, endOffset, elvisType, symbol, elvisInfo.elvisRhs
                )
            }
            return newBlock
        }

        return elvisInfo.block

    }
}

/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.ir.builders.createTmpVariable
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irIfNull
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.isTrivial
import org.jetbrains.kotlin.ir.util.shallowCopy
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * Simplifies `?.` and `?:` operator chains.
 */
class IfNullExpressionsFusionLowering(val context: CommonBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) { TODO() }

    private inner class Transformer : IrElementTransformerVoid() {
        override fun visitBlock(expression: IrBlock): IrExpression = TODO()

        // We are looking for the "if-null" expressions:
        //
        //  IfNull( E, v, A0, A1 ) =
        //      {
        //          val v = E
        //          if (v == null) A0 else A1
        //      }
        //  where
        //      E is an expression,
        //      v is a temporary variable,
        //      A0, A1 are expressions possibly containing 'v'.
        //
        // Such expressions are subject to the following rewrite rule:
        //
        // FUSE_IF_NULL:
        //  IfNull( IfNull( E, u, A0, A1 ), v, B0, B1 ) =>
        //      IfNull(
        //          E, u,
        //          SIMPLIFY_IF_NULL( A0, tmp1, B0[v <- 'tmp1'], B1[v <- 'tmp1']; { u == null }) =: C0,
        //          SIMPLIFY_IF_NULL( A1, tmp2, B0[v <- 'tmp2'], B1[v <- 'tmp2']; { u != null }) =: C1
        //      )
        //  where 'SIMPLIFY_IF_NULL( S, v, X0, X1; Context )' attempts to simplify 'IfNull( S, v, X0, X1 )' in Context,
        //  and 'X[v <- Y]' is an expression X with all occurrences of variable 'v' replaced with expression Y.
        //
        // Note that this transformation in general is not beneficial, and will in fact lead to duplication. However,
        // the structure of argument expressions in case of '?.' and '?:' operators often allows reductions in SIMPLIFY_IF_NULL.
        // This transformation is applied only if it is not necessary to copy any non-trivial subexpression:
        // if B1 is trivial or either A0 or A1 is always null, and B0 is trivial or either A0 or A1 is never null.
        // ("Trivial" is not precisely defined, but a trivial expression should have no side effects and be reasonably small.)
        //
        // Example:
        //
        //      a?.x ?: b?.y ?: z
        //
        // is translated to
        //      IfNull(
        //          IfNull(
        //              IfNull( 'a', t1, 'null', 't1.x' ),
        //              t2,
        //              IfNull( 'b', t3, 'null', 't3.y' ),
        //              't2'
        //          ),
        //          t0, 'z', 't0'
        //      )
        // which, assuming 'a!!.x' and 'b!!.y' are stable not-null expressions, is optimized to
        //      IfNull(
        //          'a', t1,
        //          IfNull( 'b', t2, 'z', 't2.y' ),
        //          't1.x'
        //      )
        // by applying FUSE_IF_NULL twice: in `a?.x ?: [...]` A0 is non-null and A1 is null, while in `[...] ?: z`
        // A1 is non-null and B1 is trivial (read of a temporary variable that stores the subject of the branch).
        private fun IrBlock.fuseIfNull(): IrExpression { TODO() }

        private fun IfNullExpr.substitute(subject: IrExpression, knownNullability: Boolean?, temporaryVarType: IrType): IrExpression = TODO()

        private fun IfNullExpr.createIrBuilder() =
            context.createIrBuilder((subjectVar.parent as IrSymbolOwner).symbol, subjectVar.startOffset, subjectVar.endOffset)

        private fun IrExpression.isNull(knownVariableSymbol: IrVariableSymbol, knownVariableIsNull: Boolean): Boolean? = TODO()
    }

    private class IfNullExpr(
        val type: IrType,
        val subjectVar: IrVariable,
        val ifNullExpr: IrExpression,
        val ifNotNullExpr: IrExpression
    )

    private fun IrExpression.matchIfNullExpr(): IfNullExpr? { TODO() }

    private fun IrBranch.getElseBranchResultOrNull(): IrExpression? { TODO() }

    private fun IrExpression.copyIfTrivial() =
        if (isTrivial()) {
            shallowCopy()
        } else this

    private fun IrExpression.remap(from: IrVariable, to: Lazy<IrVariable>): IrExpression = TODO()

    private fun IrExpression.remap(from: IrVariable, to: IrExpression): IrExpression = TODO()
}

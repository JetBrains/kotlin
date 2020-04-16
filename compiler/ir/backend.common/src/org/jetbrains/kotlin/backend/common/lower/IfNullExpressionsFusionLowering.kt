/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*

val ifNullExpressionsFusionPhase =
    makeIrFilePhase(
        ::IfNullExpressionsFusionLowering,
        name = "IfNullExpressionsFusionLowering",
        description = "Simplify '?.' and '?:' operator chains"
    )

class IfNullExpressionsFusionLowering(val context: CommonBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(Transformer(irFile))
    }

    private inner class Transformer(private val currentFile: IrFile) : IrElementTransformerVoid() {
        override fun visitBlock(expression: IrBlock): IrExpression =
            visitExpression(expression.fuseIfNull())

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
        private fun IrBlock.fuseIfNull(): IrExpression {
            val outer = matchIfNullExpr() ?: return this
            // We are going to erase 1st variable. Do so only if it is temporary (true for variables introduced for '?.' and '?:').
            if (outer.subjectVar.origin != IrDeclarationOrigin.IR_TEMPORARY_VARIABLE) return this
            val inner = outer.subjectVar.initializer?.matchIfNullExpr() ?: return this

            val innerKeepsNull = inner.ifNullExpr.isNull(inner.subjectVar.symbol, true)
            val innerDiscardsNonNull = inner.ifNotNullExpr.isNull(inner.subjectVar.symbol, false)
            if ((!outer.ifNotNullExpr.isTrivial() && innerKeepsNull != true && innerDiscardsNonNull != true) ||
                (!outer.ifNullExpr.isTrivial() && innerKeepsNull != false && innerDiscardsNonNull != false)
            ) return this
            return inner.createIrBuilder().irBlock {
                val ifNull = outer.substitute(inner.ifNullExpr, innerKeepsNull)
                val ifNotNull = outer.substitute(inner.ifNotNullExpr, innerDiscardsNonNull)
                +inner.subjectVar
                +irIfNull(outer.type, irGet(inner.subjectVar), ifNull, ifNotNull)
            }
        }

        private fun IfNullExpr.substitute(subject: IrExpression, knownNullability: Boolean?): IrExpression =
            when (knownNullability) {
                null -> createIrBuilder().irBlock {
                    val tmp = createTmpVariable(subject)
                    val ifNull = ifNullExpr.remap(subjectVar, lazy { tmp })
                    val ifNotNull = ifNotNullExpr.remap(subjectVar, lazy { tmp })
                    +irIfNull(type, irGet(tmp), ifNull, ifNotNull)
                }
                true -> ifNullExpr.remap(subjectVar, subject)
                else -> ifNotNullExpr.remap(subjectVar, subject)
            }

        private fun IfNullExpr.createIrBuilder() =
            context.createIrBuilder((subjectVar.parent as IrSymbolOwner).symbol, subjectVar.startOffset, subjectVar.endOffset)

        private fun IrExpression.isNull(knownVariableSymbol: IrVariableSymbol, knownVariableIsNull: Boolean): Boolean? {
            when (this) {
                is IrConst<*> ->
                    return value == null
                is IrGetValue -> {
                    if (symbol == knownVariableSymbol) return knownVariableIsNull
                    if (!type.isNullable()) return false
                }
                is IrConstructorCall,
                is IrGetSingletonValue,
                is IrFunctionExpression,
                is IrCallableReference,
                is IrClassReference,
                is IrGetClass ->
                    return false
                is IrCall ->
                    if (!type.isNullable() && isStableCall()) return false
            }
            return null
        }

        // TODO make calls to the declarations within the same module "stable"
        private fun IrCall.isStableCall() =
            symbol.owner.fileOrNull == currentFile
    }

    private class IfNullExpr(
        val type: IrType,
        val subjectVar: IrVariable,
        val ifNullExpr: IrExpression,
        val ifNotNullExpr: IrExpression
    )

    private fun IrExpression.matchIfNullExpr(): IfNullExpr? {
        if (this !is IrBlock) return null
        if (statements.size != 2) return null

        val subjectVar = statements[0] as? IrVariable ?: return null
        if (subjectVar.isVar) return null
        if (subjectVar.initializer == null) return null

        val whenExpr = statements[1] as? IrWhen ?: return null
        if (whenExpr.branches.size != 2) return null

        val branch0 = whenExpr.branches[0]
        val condition0 = branch0.condition as? IrCall ?: return null
        if (condition0.symbol != context.irBuiltIns.eqeqSymbol) return null
        val arg0 = condition0.getValueArgument(0) as? IrGetValue ?: return null
        if (arg0.symbol != subjectVar.symbol) return null
        val arg1 = condition0.getValueArgument(1) as? IrConst<*> ?: return null
        if (arg1.value != null) return null

        val branch1 = whenExpr.branches[1] as? IrElseBranch ?: return null

        return IfNullExpr(whenExpr.type, subjectVar, branch0.result, branch1.result)
    }

    private fun IrExpression.isTrivial() =
        this is IrExpressionWithCopy

    private fun IrExpression.copyIfTrivial() =
        if (this is IrExpressionWithCopy) copy() else this

    private fun IrExpression.remap(from: IrVariable, to: Lazy<IrVariable>): IrExpression =
        copyIfTrivial().transform(object : AbstractVariableRemapper() {
            override fun remapVariable(value: IrValueDeclaration): IrValueDeclaration? =
                if (value.symbol == from.symbol) to.value else null
        }, null)

    private fun IrExpression.remap(from: IrVariable, to: IrExpression): IrExpression =
        when {
            this is IrGetValue && symbol == from.symbol -> to
            this is IrTypeOperatorCall ->
                IrTypeOperatorCallImpl(startOffset, endOffset, type, operator, typeOperand, argument.remap(from, to))
            // TODO other expressions may not require a temporary even if they read the variable.
            else ->
                context.createIrBuilder((from.parent as IrSymbolOwner).symbol, startOffset, endOffset).irBlock {
                    +remap(from, lazy { createTmpVariable(to) })
                }.let { it.statements.singleOrNull() as IrExpression? ?: it }
        }
}

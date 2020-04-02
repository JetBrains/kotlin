/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irIfNull
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedVariableDescriptor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.Name

val ifNullExpressionsFusionPhase =
    makeIrFilePhase(
        ::IfNullExpressionsFusionLowering,
        name = "IfNullExpressionsFusionLowering",
        description = "Simplify '?.' and '?:' operator chains"
    )

class IfNullExpressionsFusionLowering(val context: CommonBackendContext) : FileLoweringPass {

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(Transformer(irFile))
        irFile.patchDeclarationParents()
    }

    private inner class Transformer(private val currentFile: IrFile) : IrElementTransformerVoid() {
        override fun visitBlock(expression: IrBlock): IrExpression =
            expression.transformPostfix {
                fuseIfNullExpressions(expression)
            }

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
        // Note that this transformation in general is not beneficial. 
        // However, the structure of argument expressions in case of '?.' and '?:' operators often allows reductions in SIMPLIFY_IF_NULL.
        // This transformation is applied only if
        //      size(A0) + size(A1) + size(B0) + size(B1) + 1 >= size(C0) + size(C1)
        // where
        //      size(X) is number of nodes in IR tree for X,
        // thus assuring that we don't grow trees exponentially.
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
        // which, assuming 'a.x' and 'b.y' are stable not-null expressions, is optimized to
        //      IfNull(
        //          'a', t1,
        //          IfNull( 'b', t2, 'z', 't2.y' ),
        //          't1.x'
        //      )
        // by applying FUSE_IF_NULL twice.
        //
        private fun fuseIfNullExpressions(expression: IrBlock) {
            val ifNull1 = expression.matchIfNullExpr() ?: return
            val ifNull2 = ifNull1.subjectExpr.matchIfNullExpr() ?: return

            val u = ifNull1.subjectVar
            // We are going to erase 1st variable. Do so only if it is temporary (true for variables introduced for '?.' and '?:').
            if (u.origin != IrDeclarationOrigin.IR_TEMPORARY_VARIABLE) return

            val a0 = ifNull2.ifNullExpr
            val a1 = ifNull2.ifNotNullExpr
            val tmp1 = createTemporaryVar(a0)
            val tmp2 = createTemporaryVar(a1)

            val b0 = ifNull1.ifNullExpr
            val b0tmp1 = b0.substituteVariable(u.symbol, tmp1.symbol)
            val b0tmp2 = b0.substituteVariable(u.symbol, tmp2.symbol)

            val b1 = ifNull1.ifNotNullExpr
            val b1tmp1 = b1.substituteVariable(u.symbol, tmp1.symbol)
            val b1tmp2 = b1.substituteVariable(u.symbol, tmp2.symbol)

            val v = ifNull2.subjectVar
            val c0 = simplifyIfNull(tmp1, b0tmp1, b1tmp1, v.symbol, true)
            val c1 = simplifyIfNull(tmp2, b0tmp2, b1tmp2, v.symbol, false)

            val sizeBeforeEstimate = a0.size() + a1.size() + b0.size() + b1.size() + 1
            val sizeAfterEstimate = c0.size() + c1.size()
            if (sizeBeforeEstimate < sizeAfterEstimate) return

            val newBlock = constructIfNullExpr(v, c0, c1)

            expression.statements.clear()
            expression.statements.addAll(newBlock.statements)
        }

        private var tmpVarsCount = 0

        private fun createTemporaryVar(initializer: IrExpression): IrVariable {
            val descriptor = WrappedVariableDescriptor()
            val symbol = IrVariableSymbolImpl(descriptor)
            val irVar = IrVariableImpl(
                initializer.startOffset, initializer.endOffset,
                IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
                symbol,
                Name.identifier("tmp_f_${tmpVarsCount++}"),
                initializer.type,
                isVar = false, isConst = false, isLateinit = false
            )
            irVar.initializer = initializer
            descriptor.bind(irVar)
            return irVar
        }

        private fun simplifyIfNull(
            subjectVariable: IrVariable,
            ifNullExpr: IrExpression,
            ifNotNullExpr: IrExpression,
            knownVariableSymbol: IrVariableSymbol,
            knownVariableIsNull: Boolean
        ): IrExpression {
            val subjectExpr = subjectVariable.initializer
                ?: throw AssertionError("Subject variable should have an initializer: ${subjectVariable.render()}")

            val ifNullResultExpr = ifNullExpr.safeReplaceSubjectVariableWithSubjectExpression(subjectVariable)
                ?: return constructIfNullExpr(subjectVariable, ifNullExpr, ifNotNullExpr)

            val ifNotNullResultExpr = ifNotNullExpr.safeReplaceSubjectVariableWithSubjectExpression(subjectVariable)
                ?: return constructIfNullExpr(subjectVariable, ifNullExpr, ifNotNullExpr)

            return when {
                subjectExpr is IrConst<*> ->
                    if (subjectExpr.value == null)
                        ifNullResultExpr
                    else
                        ifNotNullResultExpr

                subjectExpr is IrGetValue && subjectExpr.symbol == knownVariableSymbol ->
                    if (knownVariableIsNull)
                        ifNullResultExpr
                    else
                        ifNotNullResultExpr

                subjectExpr is IrGetValue && !subjectExpr.type.isNullable() ->
                    ifNotNullResultExpr

                subjectExpr is IrConstructorCall ||
                        subjectExpr is IrGetSingletonValue ||
                        subjectExpr is IrFunctionExpression ||
                        subjectExpr is IrCallableReference ||
                        subjectExpr is IrClassReference ||
                        subjectExpr is IrGetClass ->
                    ifNotNullResultExpr

                subjectExpr is IrCall && !subjectExpr.type.isNullable() && subjectExpr.isStableCall() ->
                    ifNotNullResultExpr

                else ->
                    constructIfNullExpr(subjectVariable, ifNullExpr, ifNotNullExpr)
            }
        }

        private fun IrExpression.safeReplaceSubjectVariableWithSubjectExpression(v: IrVariable): IrExpression? =
            when {
                this is IrGetValue && symbol == v.symbol ->
                    v.initializer!!

                this is IrTypeOperatorCall ->
                    argument.safeReplaceSubjectVariableWithSubjectExpression(v)?.let {
                        IrTypeOperatorCallImpl(startOffset, endOffset, type, operator, typeOperand, it)
                    }

                this.containsGetValue(v) ->
                    null

                else -> this
            }

        private fun IrExpression.containsGetValue(v: IrVariable): Boolean {
            class Searcher : IrElementVisitorVoid {
                var found = false

                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitGetValue(expression: IrGetValue) {
                    if (expression.symbol == v.symbol)
                        found = true
                    else
                        expression.acceptChildrenVoid(this)
                }
            }

            return Searcher().also { acceptChildrenVoid(it) }.found
        }

        // TODO make calls to the declarations within the same module "stable"
        private fun IrCall.isStableCall() =
            symbol.owner.fileOrNull == currentFile
    }

    private class IfNullExpr(
        val subjectVar: IrVariable,
        val ifNullExpr: IrExpression,
        val ifNotNullExpr: IrExpression
    ) {
        val subjectExpr: IrExpression = subjectVar.initializer!!
    }

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

        return IfNullExpr(
            subjectVar,
            branch0.result,
            branch1.result
        )
    }

    private fun IrExpression.substituteVariable(fromVar: IrVariableSymbol, toVar: IrVariableSymbol): IrExpression {
        val symbolRemapper = DeepCopySymbolRemapper().also { acceptVoid(it) }
        val typeRemapper = DeepCopyTypeRemapper(symbolRemapper)

        class Substitutor : DeepCopyIrTreeWithSymbols(symbolRemapper, typeRemapper) {

            override fun getNonTransformedLoop(irLoop: IrLoop): IrLoop = irLoop

            override fun visitGetValue(expression: IrGetValue): IrGetValue =
                if (expression.symbol == fromVar)
                    IrGetValueImpl(expression.startOffset, expression.endOffset, toVar)
                else
                    super.visitGetValue(expression)
        }

        return transform(Substitutor(), null)
    }

    private fun constructIfNullExpr(
        subjectVariable: IrVariable,
        ifNullExpr: IrExpression,
        ifNotNullExpr: IrExpression
    ): IrContainerExpression =
        context.createIrBuilder(subjectVariable.symbol, subjectVariable.startOffset, subjectVariable.endOffset)
            .irBlock {
                +subjectVariable
                +irIfNull(
                    ifNullExpr.type,
                    irGet(subjectVariable),
                    ifNullExpr,
                    ifNotNullExpr
                )
            }

    private fun IrExpression.size(): Int {
        class ChildrenCounter : IrElementVisitorVoid {
            var count = 0
            override fun visitElement(element: IrElement) {
                ++count
                element.acceptChildrenVoid(this)
            }
        }

        return ChildrenCounter().also { acceptVoid(it) }.count
    }
}
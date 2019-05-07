/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/** Look for when-constructs where subject is enum entry.
 * Replace branches that are comparisons with compile-time known enum entries
 * with comparisons of ordinals.
 */
class EnumWhenLowering(private val context: CommonBackendContext) : IrElementTransformerVoidWithContext(), FileLoweringPass {

    private val subjectWithOrdinalStack = mutableListOf<Pair<IrVariable, Lazy<IrVariable>>>()

    private val areEqual = context.irBuiltIns.eqeqSymbol

    private fun IrEnumEntry.ordinal(): Int {
        val result = parentAsClass.declarations.filterIsInstance<IrEnumEntry>().indexOf(this)
        assert(result >= 0) { "enum entry ${symbol.owner.dump()} not in parent class" }
        return result
    }

    override fun lower(irFile: IrFile) {
        visitFile(irFile)
    }

    override fun visitBlock(expression: IrBlock): IrExpression {
        // NB: See BranchingExpressionGenerator to get insight about `when` block translation to IR.
        if (expression.origin != IrStatementOrigin.WHEN) {
            return super.visitBlock(expression)
        }
        // when-block with subject should have two children: temporary variable and when itself.
        if (expression.statements.size != 2) {
            return super.visitBlock(expression)
        }
        val subject = expression.statements[0]
        if (subject !is IrVariable || subject.type.getClass()?.kind != ClassKind.ENUM_CLASS) {
            return super.visitBlock(expression)
        }
        // Will be initialized only when we found a branch that compares
        // subject with compile-time known enum entry.
        val subjectOrdinalProvider = lazy {
            val ordinalPropertyGetter = subject.type.getClass()!!.symbol.getPropertyGetter("ordinal")!!
            context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, subject.startOffset, subject.endOffset).run {
                val ordinal = irCall(ordinalPropertyGetter.owner).apply { dispatchReceiver = irGet(subject) }
                val integer = if (subject.type.isNullable())
                    irIfNull(ordinal.type, irGet(subject), irInt(-1), ordinal)
                else
                    ordinal
                scope.createTemporaryVariable(integer).also {
                    expression.statements.add(1, it)
                }
            }
        }
        subjectWithOrdinalStack.push(Pair(subject, subjectOrdinalProvider))
        try {
            // Process nested `when` and comparisons.
            expression.statements[1].transformChildrenVoid(this)
        } finally {
            subjectWithOrdinalStack.pop()
        }
        return expression
    }

    override fun visitCall(expression: IrCall): IrExpression {
        // We are looking for branch that is a comparison of the subject and another enum entry.
        if (expression.symbol != context.irBuiltIns.eqeqSymbol) {
            return super.visitCall(expression)
        }
        val lhs = expression.getValueArgument(0)!!
        val rhs = expression.getValueArgument(1)!!

        val (topmostSubject, topmostOrdinalProvider) = subjectWithOrdinalStack.peek()
            ?: return super.visitCall(expression)
        val other = when {
            lhs is IrGetValue && lhs.symbol.owner == topmostSubject -> rhs
            rhs is IrGetValue && rhs.symbol.owner == topmostSubject -> lhs
            else -> return super.visitCall(expression)
        }
        val entryOrdinal = when {
            other is IrGetEnumValue && topmostSubject.type.classifierOrNull?.owner == other.symbol.owner.parent ->
                other.symbol.owner.ordinal()
            other.isNullConst() ->
                -1
            else -> return super.visitCall(expression)
        }
        val subjectOrdinal = topmostOrdinalProvider.value
        return IrCallImpl(expression.startOffset, expression.endOffset, expression.type, expression.symbol).apply {
            putValueArgument(0, IrGetValueImpl(lhs.startOffset, lhs.endOffset, subjectOrdinal.type, subjectOrdinal.symbol))
            putValueArgument(1, IrConstImpl.int(rhs.startOffset, rhs.endOffset, context.irBuiltIns.intType, entryOrdinal))
        }
    }
}

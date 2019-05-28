/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
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
open class EnumWhenLowering(protected val context: CommonBackendContext) : IrElementTransformerVoidWithContext(), FileLoweringPass {
    private val subjectWithOrdinalStack = mutableListOf<Pair<IrVariable, Lazy<IrVariable>>>()

    protected open fun mapConstEnumEntry(entry: IrEnumEntry): Int =
        entry.parentAsClass.declarations.filterIsInstance<IrEnumEntry>().indexOf(this as IrEnumEntry).also {
            assert(it >= 0) { "enum entry ${entry.dump()} not in parent class" }
        }

    protected open fun mapRuntimeEnumEntry(builder: IrBuilderWithScope, subject: IrExpression): IrExpression =
        builder.irCall(subject.type.getClass()!!.symbol.getPropertyGetter("ordinal")!!).apply { dispatchReceiver = subject }

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
            context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, subject.startOffset, subject.endOffset).run {
                val integer = if (subject.type.isNullable())
                    irIfNull(context.irBuiltIns.intType, irGet(subject), irInt(-1), mapRuntimeEnumEntry(this, irGet(subject)))
                else
                    mapRuntimeEnumEntry(this, irGet(subject))
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
                mapConstEnumEntry(other.symbol.owner)
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

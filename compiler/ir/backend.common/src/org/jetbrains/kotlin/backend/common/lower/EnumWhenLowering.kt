/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
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
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.util.*

/** Look for when-constructs where subject is enum entry.
 * Replace branches that are comparisons with compile-time known enum entries
 * with comparisons of ordinals.
 */
open class EnumWhenLowering(protected open val context: CommonBackendContext) : IrElementTransformerVoidWithContext(), FileLoweringPass {

    protected open fun mapConstEnumEntry(entry: IrEnumEntry): Int =
        entry.parentAsClass.declarations.filterIsInstance<IrEnumEntry>().indexOf(entry).also {
            assert(it >= 0) { "enum entry ${entry.dump()} not in parent class" }
        }

    protected open fun mapRuntimeEnumEntry(builder: IrBuilderWithScope, subject: IrExpression): IrExpression =
        builder.irCall(subject.type.getClass()!!.symbol.getPropertyGetter("ordinal")!!).apply { dispatchReceiver = subject }

    override fun lower(irFile: IrFile) {
        visitFile(irFile)
    }

    override fun visitBlock(expression: IrBlock): IrExpression {
        expression.transformChildrenVoid()

        // NB: See BranchingExpressionGenerator to get insight about `when` block translation to IR.
        if (expression.origin != IrStatementOrigin.WHEN) {
            return expression
        }
        // when-block with subject should have two children: temporary variable and when itself.
        if (expression.statements.size != 2) {
            return expression
        }
        val subject = expression.statements[0] as? IrVariable
            ?: return expression
        val subjectClass = subject.type.getClass()
        if (subjectClass == null || subjectClass.kind != ClassKind.ENUM_CLASS || subjectClass.isEffectivelyExternal()) {
            return expression
        }
        val irWhen = expression.statements[1] as? IrWhen
            ?: return expression

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
        if (possibleToGenerateJumpTable(irWhen, subject)) {
            transformBranches(irWhen, subject, subjectOrdinalProvider)
        }
        return expression
    }

    private fun possibleToGenerateJumpTable(irWhen: IrWhen, subject: IrVariable): Boolean {
        for (irBranch in irWhen.branches) {
            val condition = irBranch.condition as? IrCall ?: continue
            if (condition.symbol != context.irBuiltIns.eqeqSymbol)
                return false

            val lhs = condition.getValueArgument(0)!!
            val rhs = condition.getValueArgument(1)!!
            val other = getOther(lhs, rhs, subject)
            if (other is IrCall) {
                return false
            }
        }
        return true
    }

    private fun getOther(lhs: IrExpression, rhs: IrExpression, subject: IrVariable): IrExpression? {
        return when {
            lhs is IrGetValue && lhs.symbol.owner == subject ->
                rhs
            rhs is IrGetValue && rhs.symbol.owner == subject ->
                lhs
            else ->
                return null
        }
    }

    private fun transformBranches(
        irWhen: IrWhen,
        subject: IrVariable,
        subjectOrdinalProvider: Lazy<IrVariable>
    ): IrExpression {
        for (irBranch in irWhen.branches) {
            irBranch.condition = transformBranchSubexpression(irBranch.condition, subject, subjectOrdinalProvider)
            irBranch.result = transformBranchSubexpression(irBranch.result, subject, subjectOrdinalProvider)
        }
        return irWhen
    }

    private fun transformBranchSubexpression(
        irExpression: IrExpression,
        subject: IrVariable,
        subjectOrdinalProvider: Lazy<IrVariable>
    ): IrExpression =
        when (irExpression) {
            is IrCall -> {
                // Single entry clause:
                //  when (subject) {
                //      ENUM_ENTRY -> ...
                //  }
                transformEnumEquals(irExpression, subject, subjectOrdinalProvider)
            }
            is IrWhen -> {
                // Multiple entry clause:
                //  when (subject) {
                //      ENUM_ENTRY_1, ENUM_ENTRY_2, ..., ENUM_ENTRY_N -> ...
                //  }
                transformBranches(irExpression, subject, subjectOrdinalProvider)
            }
            else -> irExpression
        }

    private fun transformEnumEquals(
        expression: IrCall,
        subject: IrVariable,
        subjectOrdinalProvider: Lazy<IrVariable>
    ): IrExpression {
        // We are looking for branch that is a comparison of the subject and another enum entry.
        if (expression.symbol != context.irBuiltIns.eqeqSymbol) {
            return expression
        }
        val lhs = expression.getValueArgument(0)!!
        val rhs = expression.getValueArgument(1)!!
        val other = getOther(lhs, rhs, subject) ?: return expression
        val entryOrdinal = when {
            other is IrGetEnumValue && subject.type.classifierOrNull?.owner == other.symbol.owner.parent ->
                mapConstEnumEntry(other.symbol.owner)
            other.isNullConst() ->
                -1
            else ->
                return expression
        }
        val subjectOrdinal = subjectOrdinalProvider.value
        return IrCallImpl(
            expression.startOffset, expression.endOffset,
            expression.type, expression.symbol,
            typeArgumentsCount = 0,
            valueArgumentsCount = 2
        ).apply {
            putValueArgument(0, IrGetValueImpl(lhs.startOffset, lhs.endOffset, subjectOrdinal.type, subjectOrdinal.symbol))
            putValueArgument(1, IrConstImpl.int(rhs.startOffset, rhs.endOffset, context.irBuiltIns.intType, entryOrdinal))
        }
    }

}

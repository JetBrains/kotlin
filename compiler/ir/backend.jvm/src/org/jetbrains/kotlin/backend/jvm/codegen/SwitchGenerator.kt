/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.StackValue.*
import org.jetbrains.kotlin.codegen.`when`.SwitchCodegen.Companion.preferLookupOverSwitch
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.isTrueConst
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Type
import java.util.*

// TODO: eliminate the temporary variable
class SwitchGenerator(private val expression: IrWhen, private val data: BlockInfo, private val codegen: ExpressionCodegen) {
    private val mv = codegen.mv

    // @return null if the IrWhen cannot be emitted as lookupswitch or tableswitch.
    fun generate(): StackValue? {
        val endLabel = Label()
        var defaultLabel = endLabel
        val thenExpressions = ArrayList<Pair<IrExpression, Label>>()
        var elseExpression: IrExpression? = null
        val allConditions = ArrayList<Pair<IrCall, Label>>()

        // Parse the when structure. Note that the condition can be nested. See matchConditions() for details.
        for (branch in expression.branches) {
            if (branch is IrElseBranch) {
                elseExpression = branch.result
                defaultLabel = Label()
            } else {
                val conditions = matchConditions(branch.condition) ?: return null
                val thenLabel = Label()
                thenExpressions.add(Pair(branch.result, thenLabel))
                allConditions += conditions.map { Pair(it, thenLabel) }
            }
        }

        // IF is more compact when there are only 1 or fewer branches, in addition to else.
        if (allConditions.size <= 1)
            return null

        if (areConstIntComparisons(allConditions.map { it.first })) {
            // if all conditions are CALL EQEQ(tmp_variable, some_int_constant)
            val cases = allConditions.mapTo(ArrayList()) { Pair((it.first.getValueArgument(1) as IrConst<*>).value as Int, it.second) }
            val subject = allConditions[0].first.getValueArgument(0)!! as IrGetValue
            return gen(cases, subject, defaultLabel, endLabel, elseExpression, thenExpressions)
        }

        // TODO: String, Enum, etc.
        return null
    }

    // A lookup/table switch can be used if...
    private fun areConstIntComparisons(conditions: List<IrCall>): Boolean {
        // 1. All branches are CALL 'EQEQ(Any?, Any?)': Boolean
        if (conditions.any { it.symbol != codegen.classCodegen.context.irBuiltIns.eqeqSymbol })
            return false

        // 2. All types of variables involved in comparison are Int.
        // 3. All arg0 refer to the same value.
        val lhs = conditions.map { it.getValueArgument(0) as? IrGetValue }
        if (lhs.any { it == null || it.symbol != lhs[0]!!.symbol || !it.type.isInt() })
            return false

        // 4. All arg1 are IrConst<*>.
        val rhs = conditions.map { it.getValueArgument(1) as? IrConst<*> }
        if (rhs.any { it == null || it.kind != IrConstKind.Int })
            return false

        return true
    }

    // psi2ir lowers multiple cases to nested conditions. For example,
    //
    // when (subject) {
    //   a, b, c -> action
    // }
    //
    // is lowered to
    //
    // if (if (subject == a)
    //       true
    //     else
    //       if (subject == b)
    //         true
    //       else
    //         subject == c) {
    //     action
    // }
    //
    // @return true if the conditions are equality checks of constants.
    private fun matchConditions(condition: IrExpression): ArrayList<IrCall>? {
        if (condition is IrCall) {
            return arrayListOf(condition)
        } else if (condition is IrWhen && condition.origin == IrStatementOrigin.WHEN_COMMA) {
            assert(condition.type.isBoolean()) { "WHEN_COMMA should always be a Boolean: ${condition.dump()}" }

            val candidates = ArrayList<IrCall>()

            // Match the following structure:
            //
            // when() {
            //   cond_1 -> true
            //   cond_2 -> true
            //   ...
            //   else -> cond_N
            // }
            //
            // Namely, the structure which returns true if any one of the condition is true.
            for (branch in condition.branches) {
                if (branch is IrElseBranch) {
                    assert(branch.condition.isTrueConst()) { "IrElseBranch.condition should be const true: ${branch.condition.dump()}" }
                    candidates += matchConditions(branch.result) ?: return null
                } else {
                    if (!branch.result.isTrueConst())
                        return null
                    candidates += matchConditions(branch.condition) ?: return null
                }
            }

            return if (candidates.isNotEmpty()) candidates else return null
        }

        return null
    }

    private fun gen(expression: IrElement, data: BlockInfo): StackValue = codegen.gen(expression, data)

    private fun coerceNotToUnit(fromType: Type, fromKotlinType: KotlinType?, toKotlinType: KotlinType): StackValue =
        codegen.coerceNotToUnit(fromType, fromKotlinType, toKotlinType)

    private fun gen(
        cases: ArrayList<Pair<Int, Label>>,
        subject: IrGetValue,
        defaultLabel: Label,
        endLabel: Label,
        elseExpression: IrExpression?,
        thenExpressions: ArrayList<Pair<IrExpression, Label>>
    ): StackValue {
        cases.sortBy { it.first }

        // Emit the temporary variable for subject.
        gen(subject, data)

        val caseMin = cases.first().first
        val caseMax = cases.last().first
        val rangeLength = caseMax - caseMin + 1L

        // Emit either tableswitch or lookupswitch, depending on the code size.
        //
        // lookupswitch is 2X as large as tableswitch with the same entries. However, lookupswitch is sparse while tableswitch must
        // enumerate all the entries in the range.
        if (preferLookupOverSwitch(cases.size, rangeLength)) {
            mv.lookupswitch(defaultLabel, cases.map { it.first }.toIntArray(), cases.map { it.second }.toTypedArray())
        } else {
            val labels = Array(rangeLength.toInt()) { defaultLabel }
            for (case in cases)
                labels[case.first - caseMin] = case.second
            mv.tableswitch(caseMin, caseMax, defaultLabel, *labels)
        }

        // all entries except else
        for (thenExpression in thenExpressions) {
            mv.visitLabel(thenExpression.second)
            val stackValue = thenExpression.first.run { gen(this, data) }
            coerceNotToUnit(stackValue.type, stackValue.kotlinType, expression.type.toKotlinType())
            mv.goTo(endLabel)
        }

        // else
        val result = if (elseExpression == null) {
            // There's no else part. No stack value will be generated.
            StackValue.putUnitInstance(mv)
            onStack(Type.VOID_TYPE)
        } else {
            // Generate the else part.
            mv.visitLabel(defaultLabel)
            val stackValue = elseExpression.run { gen(this, data) }
            coerceNotToUnit(stackValue.type, stackValue.kotlinType, expression.type.toKotlinType())
        }

        mv.mark(endLabel)
        return result
    }
}


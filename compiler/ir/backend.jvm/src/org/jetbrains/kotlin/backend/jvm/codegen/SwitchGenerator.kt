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

    data class ExpressionToLabel(val expression: IrExpression, val label: Label)
    data class CallToLabel(val call: IrCall, val label: Label)
    data class ValueToLabel(val value: Any?, val label: Label)

    // @return null if the IrWhen cannot be emitted as lookupswitch or tableswitch.
    fun generate(): StackValue? {
        val endLabel = Label()
        var defaultLabel = endLabel
        val expressionToLabels = ArrayList<ExpressionToLabel>()
        var elseExpression: IrExpression? = null
        val callToLabels = ArrayList<CallToLabel>()

        // Parse the when structure. Note that the condition can be nested. See matchConditions() for details.
        for (branch in expression.branches) {
            if (branch is IrElseBranch) {
                elseExpression = branch.result
                defaultLabel = Label()
            } else {
                val conditions = matchConditions(branch.condition) ?: return null
                val thenLabel = Label()
                expressionToLabels.add(ExpressionToLabel(branch.result, thenLabel))
                callToLabels += conditions.map { CallToLabel(it, thenLabel) }
            }
        }

        // switch isn't applicable if there's no case at all, e.g., when() { else -> ... }
        if (callToLabels.size == 0)
            return null

        val calls = callToLabels.map { it.call }

        // Checks if all conditions are CALL EQEQ(tmp_variable, some_constant)
        if (!areConstComparisons(calls))
            return null

        // Subject should be the same for all conditions. Let's pick the first.
        val subject = callToLabels[0].call.getValueArgument(0)!! as IrGetValue

        // Don't generate repeated cases, which are unreachable but allowed in Kotlin.
        // Only keep the first encountered case:
        val cases =
            callToLabels.map { ValueToLabel((it.call.getValueArgument(1) as IrConst<*>).value, it.label) }.distinctBy { it.value }

        // Remove labels and "then expressions" that are not reachable.
        val reachableLabels = HashSet(cases.map { it.label })
        expressionToLabels.removeIf { it.label !in reachableLabels }

        return when {
            areConstIntComparisons(calls) ->
                IntSwitch(
                    subject,
                    defaultLabel,
                    endLabel,
                    elseExpression,
                    expressionToLabels,
                    cases
                )
            areConstStringComparisons(calls) ->
                StringSwitch(
                    subject,
                    defaultLabel,
                    endLabel,
                    elseExpression,
                    expressionToLabels,
                    cases
                )
            else -> null // TODO: Enum, etc.
        }?.genOptimizedIfEnoughCases()
    }

    private fun areConstComparisons(conditions: List<IrCall>): Boolean {
        // All branches must be CALL 'EQEQ(Any?, Any?)': Boolean
        if (conditions.any { it.symbol != codegen.classCodegen.context.irBuiltIns.eqeqSymbol })
            return false

        // All LHS refer to the same tmp variable.
        val lhs = conditions.map { it.getValueArgument(0) as? IrGetValue }
        if (lhs.any { it == null || it.symbol != lhs[0]!!.symbol })
            return false

        // All RHS are constants
        if (conditions.any { it.getValueArgument(1) !is IrConst<*> })
            return false

        return true
    }

    private fun areConstIntComparisons(conditions: List<IrCall>): Boolean {
        return checkTypeSpecifics(conditions, { it.isInt() }, { it.kind == IrConstKind.Int })
    }

    private fun areConstStringComparisons(conditions: List<IrCall>): Boolean {
        return checkTypeSpecifics(
            conditions,
            { it.isString() || it.isNullableString() },
            { it.kind == IrConstKind.String || it.kind == IrConstKind.Null })
    }

    private fun checkTypeSpecifics(
        conditions: List<IrCall>,
        subjectTypePredicate: (IrType) -> Boolean,
        irConstPredicate: (IrConst<*>) -> Boolean
    ): Boolean {
        val lhs = conditions.map { it.getValueArgument(0) as IrGetValue }
        if (lhs.any { !subjectTypePredicate(it.type) })
            return false

        val rhs = conditions.map { it.getValueArgument(1) as IrConst<*> }
        if (rhs.any { !irConstPredicate(it) })
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

    abstract inner class Switch(
        val subject: IrGetValue,
        val defaultLabel: Label,
        val endLabel: Label,
        val elseExpression: IrExpression?,
        val expressionToLabels: ArrayList<ExpressionToLabel>
    ) {
        open fun shouldOptimize() = false

        open fun genOptimizedIfEnoughCases(): StackValue? {
            if (!shouldOptimize())
                return null

            genSubject()
            genSwitch()
            genThenExpressions()
            val result = genElseExpression()

            mv.mark(endLabel)
            return result
        }

        protected abstract fun genSubject()

        protected abstract fun genSwitch()

        protected fun genIntSwitch(unsortedIntCases: List<ValueToLabel>) {
            val intCases = unsortedIntCases.sortedBy { it.value as Int }
            val caseMin = intCases.first().value as Int
            val caseMax = intCases.last().value as Int
            val rangeLength = caseMax.toLong() - caseMin.toLong() + 1L

            // Emit either tableswitch or lookupswitch, depending on the code size.
            //
            // lookupswitch is 2X as large as tableswitch with the same entries. However, lookupswitch is sparse while tableswitch must
            // enumerate all the entries in the range.
            if (preferLookupOverSwitch(intCases.size, rangeLength)) {
                mv.lookupswitch(defaultLabel, intCases.map { it.value as Int }.toIntArray(), intCases.map { it.label }.toTypedArray())
            } else {
                val labels = Array(rangeLength.toInt()) { defaultLabel }
                for (case in intCases)
                    labels[case.value as Int - caseMin] = case.label
                mv.tableswitch(caseMin, caseMax, defaultLabel, *labels)
            }
        }

        protected fun genThenExpressions() {
            for ((thenExpression, label) in expressionToLabels) {
                mv.visitLabel(label)
                val stackValue = thenExpression.run { gen(this, data) }
                coerceNotToUnit(stackValue.type, stackValue.kotlinType, expression.type.toKotlinType())
                mv.goTo(endLabel)
            }
        }

        protected fun genElseExpression(): StackValue {
            return if (elseExpression == null) {
                // There's no else part. No stack value will be generated.
                StackValue.putUnitInstance(mv)
                onStack(Type.VOID_TYPE)
            } else {
                // Generate the else part.
                mv.visitLabel(defaultLabel)
                val stackValue = elseExpression.run { gen(this, data) }
                coerceNotToUnit(stackValue.type, stackValue.kotlinType, expression.type.toKotlinType())
            }
        }
    }

    inner class IntSwitch(
        subject: IrGetValue,
        defaultLabel: Label,
        endLabel: Label,
        elseExpression: IrExpression?,
        expressionToLabels: ArrayList<ExpressionToLabel>,
        private val cases: List<ValueToLabel>
    ) : Switch(subject, defaultLabel, endLabel, elseExpression, expressionToLabels) {

        // IF is more compact when there are only 1 or fewer branches, in addition to else.
        override fun shouldOptimize() = cases.size > 1

        override fun genSubject() {
            gen(subject, data)
        }

        override fun genSwitch() {
            genIntSwitch(cases)
        }
    }

    // The following when structure:
    //
    //   when (s) {
    //     s1, s2 -> e1,
    //     s3 -> e2,
    //     s4 -> e3,
    //     ...
    //     else -> e
    //   }
    //
    // is implemented as:
    //
    //   // if s is String?, generate the following null check:
    //   if (s == null)
    //     // jump to the case where null is handled, if defined.
    //     // otherwise, jump out of the when().
    //     ...
    //   ...
    //   when (s.hashCode()) {
    //     h1 -> {
    //       if (s == s1)
    //         e1
    //       else if (s == s2)
    //         e1
    //       else if (s == s3)
    //         e2
    //       else
    //         e
    //     }
    //     h2 -> if (s == s3) e2 else e,
    //     ...
    //     else -> e
    //   }
    //
    // where s1.hashCode() == s2.hashCode() == s3.hashCode() == h1,
    //       s4.hashCode() == h2.
    //
    // A tableswitch or lookupswitch is then used for the hash code lookup.

    inner class StringSwitch(
        subject: IrGetValue,
        defaultLabel: Label,
        endLabel: Label,
        elseExpression: IrExpression?,
        expressionToLabels: ArrayList<ExpressionToLabel>,
        private val cases: List<ValueToLabel>
    ) : Switch(subject, defaultLabel, endLabel, elseExpression, expressionToLabels) {

        private val hashToStringAndExprLabels = HashMap<Int, ArrayList<ValueToLabel>>()
        private val hashAndSwitchLabels = ArrayList<ValueToLabel>()

        init {
            for (case in cases)
                if (case.value != null) // null is handled specially and will never be dispatched from the switch.
                    hashToStringAndExprLabels.getOrPut(case.value.hashCode()) { ArrayList() }.add(
                        ValueToLabel(case.value, case.label)
                    )

            for (key in hashToStringAndExprLabels.keys)
                hashAndSwitchLabels.add(ValueToLabel(key, Label()))
        }

        // Using a switch, the subject string has to be traversed at least twice (hash + comparison * N, where N is #strings hashed into the
        // same bucket). The optimization isn't better than an IF cascade when #switch-targets <= 2.
        override fun shouldOptimize() = hashAndSwitchLabels.size > 2

        override fun genSubject() {
            if (subject.type.isNullableString()) {
                val nullLabel = cases.find { it.value == null }?.label ?: defaultLabel
                gen(subject, data)
                mv.ifnull(nullLabel)
            }
            gen(subject, data)
            mv.invokevirtual("java/lang/String", "hashCode", "()I", false)
        }

        override fun genSwitch() {
            genIntSwitch(hashAndSwitchLabels)

            // Multiple strings can be hashed into the same bucket.
            // Generate an if cascade to resolve that for each bucket.
            for ((hash, switchLabel) in hashAndSwitchLabels) {
                mv.visitLabel(switchLabel)
                for ((string, label) in hashToStringAndExprLabels[hash]!!) {
                    gen(subject, data)
                    mv.aconst(string)
                    mv.invokevirtual("java/lang/String", "equals", "(Ljava/lang/Object;)Z", false)
                    mv.ifne(label)
                }
                mv.goTo(defaultLabel)
            }
        }
    }
}

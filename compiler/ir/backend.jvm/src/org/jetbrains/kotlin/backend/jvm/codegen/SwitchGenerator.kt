/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.codegen.`when`.SwitchCodegen.Companion.preferLookupOverSwitch
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.isTrueConst
import org.jetbrains.org.objectweb.asm.Label
import java.util.*

// TODO: eliminate the temporary variable
class SwitchGenerator(private val expression: IrWhen, private val data: BlockInfo, private val codegen: ExpressionCodegen) {
    data class ExpressionToLabel(val expression: IrExpression, val label: Label)
    data class CallToLabel(val call: IrCall, val label: Label)
    data class ValueToLabel(val value: Any?, val label: Label)

    // @return null if the IrWhen cannot be emitted as lookupswitch or tableswitch.
    fun generate(): PromisedValue? {
        val expressionToLabels = ArrayList<ExpressionToLabel>()
        var elseExpression: IrExpression? = null
        val callToLabels = ArrayList<CallToLabel>()

        // Parse the when structure. Note that the condition can be nested. See matchConditions() for details.
        for (branch in expression.branches) {
            if (branch is IrElseBranch) {
                elseExpression = branch.result
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

        // To generate a switch from a when it must be a comparison of a single
        // variable, the "subject", against a series of constants. We assume the
        // subject is the left hand side of the first condition, provided the
        // first condition is a comparison. If the first condition is of the form:
        //
        //     CALL EQEQ(<unsafe-coerce><UInt, Int>(var),_)
        //
        // we must be trying to generate an _unsigned_ int switch, and need to
        // account for unsafe-coerce in all comparisons that arise from the
        // wrapping and unwrapping of the UInt inline class wrapper. Otherwise,
        // this is a primitive Int or String switch, with a condition of the form
        //
        //     CALL EQEQ(var,_)

        val firstCondition = callToLabels[0].call
        if (firstCondition.symbol != codegen.classCodegen.context.irBuiltIns.eqeqSymbol) return null
        val subject = firstCondition.getValueArgument(0)
        return when {
            subject is IrCall && subject.isCoerceFromUIntToInt() ->
                generateUIntSwitch(subject.getValueArgument(0)!! as? IrGetValue, calls, callToLabels, expressionToLabels, elseExpression)
            subject is IrGetValue ->
                generatePrimitiveSwitch(subject, calls, callToLabels, expressionToLabels, elseExpression)
            else ->
                null
        }?.genOptimizedIfEnoughCases()
    }

    fun IrCall.isCoerceFromUIntToInt(): Boolean =
        symbol == codegen.classCodegen.context.ir.symbols.unsafeCoerceIntrinsic
                && getTypeArgument(0)?.isUInt() == true
                && getTypeArgument(1)?.isInt() == true

    private fun generateUIntSwitch(
        subject: IrGetValue?,
        conditions: List<IrCall>,
        callToLabels: ArrayList<CallToLabel>,
        expressionToLabels: ArrayList<ExpressionToLabel>,
        elseExpression: IrExpression?
    ): Switch? {
        if (subject == null) return null
        // We check that all conditions are of the form
        //    CALL EQEQ (<unsafe-coerce><UInt,Int>(subject),
        //               <unsafe-coerce><UInt,Int>( Constant ))
        if (!areConstUIntComparisons(conditions)) return null

        // Filter repeated cases. Allowed in Kotlin but unreachable.
        val cases = callToLabels.map {
            val constCoercion = it.call.getValueArgument(1)!! as IrCall
            val constValue = (constCoercion.getValueArgument(0) as IrConst<*>).value
            ValueToLabel(
                constValue,
                it.label
            )
        }.distinctBy { it.value }

        expressionToLabels.removeUnreachableLabels(cases)

        return IntSwitch(
            subject,
            elseExpression,
            expressionToLabels,
            cases
        )
    }

    private fun generatePrimitiveSwitch(
        subject: IrGetValue,
        conditions: List<IrCall>,
        callToLabels: ArrayList<CallToLabel>,
        expressionToLabels: ArrayList<ExpressionToLabel>,
        elseExpression: IrExpression?
    ): Switch? {
        // Checks if all conditions are CALL EQEQ(var,constant)
        if (!areConstantComparisons(conditions)) return null

        return when {
            areConstIntComparisons(conditions) -> {
                val cases = extractSwitchCasesAndFilterUnreachableLabels(callToLabels, expressionToLabels)
                IntSwitch(
                    subject,
                    elseExpression,
                    expressionToLabels,
                    cases
                )
            }
            areConstStringComparisons(conditions) -> {
                val cases = extractSwitchCasesAndFilterUnreachableLabels(callToLabels, expressionToLabels)
                StringSwitch(
                    subject,
                    elseExpression,
                    expressionToLabels,
                    cases
                )
            }
            else ->
                null
        }
    }

    // Check that all conditions are of the form
    //
    //  CALL EQEQ (<unsafe-coerce><UInt,Int>(subject), <unsafe-coerce><UInt,Int>( Constant ))
    //
    // where subject is taken to be the first variable compared on the left hand side, if any.
    private fun areConstUIntComparisons(conditions: List<IrCall>): Boolean {
        val lhs = conditions.map { it.takeIf { it.symbol == codegen.context.irBuiltIns.eqeqSymbol }?.getValueArgument(0) as? IrCall }
        if (lhs.any { it == null || !it.isCoerceFromUIntToInt() }) return false
        val lhsVariableAccesses = lhs.map { it!!.getValueArgument(0) as? IrGetValue }
        if (lhsVariableAccesses.any { it == null || it.symbol != lhsVariableAccesses[0]!!.symbol }) return false

        val rhs = conditions.map { it.getValueArgument(1) as? IrCall }
        if (rhs.any { it == null || !it.isCoerceFromUIntToInt() || it.getValueArgument(0) !is IrConst<*> }) return false

        return true
    }

    private fun areConstantComparisons(conditions: List<IrCall>): Boolean {
        // All conditions are equality checks && all LHS refer to the same tmp variable.
        val lhs = conditions.map { it.takeIf { it.symbol == codegen.context.irBuiltIns.eqeqSymbol }?.getValueArgument(0) as? IrGetValue }
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

    private fun extractSwitchCasesAndFilterUnreachableLabels(
        callToLabels: List<CallToLabel>,
        expressionToLabels: ArrayList<ExpressionToLabel>
    ): List<ValueToLabel> {
        // Don't generate repeated cases, which are unreachable but allowed in Kotlin.
        // Only keep the first encountered case:
        val cases =
            callToLabels.map { ValueToLabel((it.call.getValueArgument(1) as IrConst<*>).value, it.label) }.distinctBy { it.value }

        expressionToLabels.removeUnreachableLabels(cases)

        return cases
    }

    private fun ArrayList<ExpressionToLabel>.removeUnreachableLabels(cases: List<ValueToLabel>) {
        val reachableLabels = HashSet(cases.map { it.label })
        removeIf { it.label !in reachableLabels }
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
                candidates += if (branch is IrElseBranch) {
                    assert(branch.condition.isTrueConst()) { "IrElseBranch.condition should be const true: ${branch.condition.dump()}" }
                    matchConditions(branch.result) ?: return null
                } else {
                    if (!branch.result.isTrueConst())
                        return null
                    matchConditions(branch.condition) ?: return null
                }
            }

            return if (candidates.isNotEmpty()) candidates else return null
        }

        return null
    }

    abstract inner class Switch(
        val subject: IrGetValue,
        val elseExpression: IrExpression?,
        val expressionToLabels: ArrayList<ExpressionToLabel>
    ) {
        protected val defaultLabel = Label()

        open fun shouldOptimize() = false

        open fun genOptimizedIfEnoughCases(): PromisedValue? {
            if (!shouldOptimize())
                return null

            genSwitch()
            return genBranchTargets()
        }

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
            with(codegen) {
                if (preferLookupOverSwitch(intCases.size, rangeLength)) {
                    mv.lookupswitch(defaultLabel, intCases.map { it.value as Int }.toIntArray(), intCases.map { it.label }.toTypedArray())
                } else {
                    val labels = Array(rangeLength.toInt()) { defaultLabel }
                    for (case in intCases)
                        labels[case.value as Int - caseMin] = case.label
                    mv.tableswitch(caseMin, caseMax, defaultLabel, *labels)
                }
            }
        }

        private fun genBranchTargets(): PromisedValue {
            with(codegen) {
                val endLabel = Label()

                for ((thenExpression, label) in expressionToLabels) {
                    mv.visitLabel(label)
                    thenExpression.accept(codegen, data).also {
                        if (elseExpression != null) {
                            it.materializedAt(expression.type)
                        } else {
                            it.discard()
                        }
                    }
                    mv.goTo(endLabel)
                }

                mv.visitLabel(defaultLabel)
                val result = elseExpression?.accept(codegen, data)?.materializedAt(expression.type) ?: unitValue
                mv.mark(endLabel)
                return result
            }
        }
    }

    inner class IntSwitch(
        subject: IrGetValue,
        elseExpression: IrExpression?,
        expressionToLabels: ArrayList<ExpressionToLabel>,
        private val cases: List<ValueToLabel>
    ) : Switch(subject, elseExpression, expressionToLabels) {

        // IF is more compact when there are only 1 or fewer branches, in addition to else.
        override fun shouldOptimize() = cases.size > 1

        override fun genSwitch() {
            // Do not generate line numbers for the table switching. In particular,
            // the subject is extracted from the condition of the first branch which
            // will give the wrong stepping behavior for code such as:
            //
            // when {
            //   x == 42 -> 1
            //   x == 32 -> 2
            //   x == 24 -> 3
            //   ...
            // }
            //
            // If the subject line number is generated, we will not stop on the line
            // of the `when` but instead stop on the `x == 42` line. When x is 24,
            // we would stop on the line `x == 42` and then step to the line `x == 24`.
            // That is confusing and we prefer to stop on the `when` line and then step
            // to the `x == 24` line. This is accomplished by ignoring the line number
            // information for the subject as the `when` line number has already been
            // emitted.
            codegen.noLineNumberScope {
                subject.accept(codegen, data).materialize()
            }
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
        elseExpression: IrExpression?,
        expressionToLabels: ArrayList<ExpressionToLabel>,
        private val cases: List<ValueToLabel>
    ) : Switch(subject, elseExpression, expressionToLabels) {

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

        override fun genSwitch() {
            with(codegen) {
                // Do not generate line numbers for the table switching. In particular,
                // the subject is extracted from the condition of the first branch which
                // will give the wrong stepping behavior for code such as:
                //
                // when {
                //   x == "x" -> 1
                //   x == "y" -> 2
                //   x == "z" -> 3
                //   ...
                // }
                //
                // If the subject line number is generated, we will not stop on the line
                // of the `when` but instead stop on the `x == "x"` line. When x is "z",
                // we would stop on the line `x == "x"` and then step to the line `x == "z"`.
                // That is confusing and we prefer to stop on the `when` line and then step
                // to the `x == "z"` line. This is accomplished by ignoring the line number
                // information for the subject as the `when` line number has already been
                // emitted.
                noLineNumberScope {
                    if (subject.type.isNullableString()) {
                        subject.accept(codegen, data).materialize()
                        mv.ifnull(cases.find { it.value == null }?.label ?: defaultLabel)
                    }
                    // Reevaluating the subject is fine here because it is a read of a temporary.
                    subject.accept(codegen, data).materialize()
                    mv.invokevirtual("java/lang/String", "hashCode", "()I", false)
                }
                genIntSwitch(hashAndSwitchLabels)

                // Multiple strings can be hashed into the same bucket.
                // Generate an if cascade to resolve that for each bucket.
                for ((hash, switchLabel) in hashAndSwitchLabels) {
                    mv.visitLabel(switchLabel)
                    for ((string, label) in hashToStringAndExprLabels[hash]!!) {
                        noLineNumberScope {
                            subject.accept(codegen, data).materialize()
                        }
                        mv.aconst(string)
                        mv.invokevirtual("java/lang/String", "equals", "(Ljava/lang/Object;)Z", false)
                        mv.ifne(label)
                    }
                    mv.goTo(defaultLabel)
                }
            }
        }
    }
}

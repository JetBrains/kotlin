/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

enum class ConditionValue(val token: String) {
    True("true"), False("false"), Null("null");

    override fun toString(): String {
        return token
    }

    fun invert(): ConditionValue? = when (this) {
        True -> False
        False -> True
        else -> null
    }
}

fun Boolean.toConditionValue(): ConditionValue = if (this) ConditionValue.True else ConditionValue.False

enum class ConditionOperator(val token: String) {
    Eq("=="), NotEq("!=");

    fun invert(): ConditionOperator = when (this) {
        Eq -> NotEq
        NotEq -> Eq
    }

    override fun toString(): String {
        return token
    }
}

class Condition(val variable: DataFlowVariable, val rhs: ConditionRHS) {
    val operator: ConditionOperator get() = rhs.operator
    val value: ConditionValue get() = rhs.value

    constructor(
        variable: DataFlowVariable,
        operator: ConditionOperator,
        value: ConditionValue
    ) : this(variable, ConditionRHS(operator, value))

    override fun toString(): String {
        return "$variable $rhs"
    }
}

data class ConditionRHS(val operator: ConditionOperator, val value: ConditionValue) {
    fun invert(): ConditionRHS {
        val newValue = value.invert()
        return if (newValue != null) {
            ConditionRHS(operator, newValue)
        } else {
            ConditionRHS(operator.invert(), value)
        }
    }

    override fun toString(): String {
        return "$operator $value"
    }
}


internal fun eq(value: ConditionValue): ConditionRHS = ConditionRHS(ConditionOperator.Eq, value)
internal fun notEq(value: ConditionValue): ConditionRHS = ConditionRHS(ConditionOperator.NotEq, value)
internal infix fun DataFlowVariable.eq(value: ConditionValue): Condition =
    Condition(this, org.jetbrains.kotlin.fir.resolve.dfa.eq(value))
internal infix fun DataFlowVariable.notEq(value: ConditionValue): Condition =
    Condition(this, org.jetbrains.kotlin.fir.resolve.dfa.notEq(value))
// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION -DEBUG_INFO_SMARTCAST
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-213
 * PLACE: control--and-data-flow-analysis, control-flow-graph, expressions-1, conditional-expressions -> paragraph 1 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: check any if-statement in kotlin may be trivially turned into such an expression by replacing the missing branch with a kotlin.Unit object expression.
 * HELPERS: checkType, functions
 */

// TESTCASE NUMBER: 1

fun case1() {
    val b = true

    if (!b) { 123 } //statement

    val expression: Any = if (!b) {  } else kotlin.Unit

    expression checkType { check<Any>() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>expression<!>

    expression as kotlin.Unit
    expression checkType { check<Unit>() }
}

// TESTCASE NUMBER: 2

fun case2() {
    val a = 1
    val b = 2
    if (a > b) { a } //statement
    val expression: Any = if (a > b) { a } else { }

    expression checkType { check<Any>() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>expression<!>
    expression as  kotlin.Unit
    expression checkType { check<Unit>() }
}
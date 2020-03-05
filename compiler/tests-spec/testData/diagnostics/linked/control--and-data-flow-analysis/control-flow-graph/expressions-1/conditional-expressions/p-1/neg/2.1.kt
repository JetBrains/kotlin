// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-213
 * PLACE: control--and-data-flow-analysis, control-flow-graph, expressions-1, conditional-expressions -> paragraph 1 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: check any if-statement in kotlin may be trivially turned into such an expression by replacing the missing branch with a kotlin.Unit object expression.
 * HELPERS: checkType
 */

// TESTCASE NUMBER: 1

fun case1() {
    val b = true
    if (!b) {
        println("this is statement")
    }
    val statement = <!INVALID_IF_AS_EXPRESSION!>if<!> (!b) { println("statement could not be assigned") }
}

// TESTCASE NUMBER: 2

fun case2() {
    val a = 1
    val b = 2
    if (a > b) a else ; //statement
    val expression: Any = <!INVALID_IF_AS_EXPRESSION!>if<!> (a > b) a else ;
}
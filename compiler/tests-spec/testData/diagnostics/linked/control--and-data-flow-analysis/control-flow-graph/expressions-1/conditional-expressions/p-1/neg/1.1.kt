// !LANGUAGE: +NewInference
// !DIAGNOSTICS:  -IMPLICIT_CAST_TO_ANY -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-296
 * PLACE: control--and-data-flow-analysis, control-flow-graph, expressions-1, conditional-expressions -> paragraph 1 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: check if-expressions must have both branches.
 */

// TESTCASE NUMBER: 1

fun case1() {
    val b = true
    val a =  <!INVALID_IF_AS_EXPRESSION!>if<!> (b) {
        "true"
    }
}

// TESTCASE NUMBER: 2

fun case2() {
    val b = true
    val a = <!INVALID_IF_AS_EXPRESSION!>if<!> (b) "true"
}


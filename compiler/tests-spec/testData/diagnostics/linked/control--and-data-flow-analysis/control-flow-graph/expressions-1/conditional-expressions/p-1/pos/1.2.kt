// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -IMPLICIT_CAST_TO_ANY -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION -DEBUG_INFO_SMARTCAST
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-213
 * PLACE: control--and-data-flow-analysis, control-flow-graph, expressions-1, conditional-expressions -> paragraph 1 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: check if-expressions must have both branches.
 * HELPERS: checkType
 */


// TESTCASE NUMBER: 1

fun case1() {
    val b = true
    val c = true
    val a = if (b) {
        "first true"
    } else if (c) {
        "else if true"
    } else {}
}

// TESTCASE NUMBER: 2

fun case2() {
    var b = true
    val c = true
    val a = if (b) 1 else if (c) 2 else Unit;
}

// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION -DEBUG_INFO_SMARTCAST
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-213
 * PLACE: control--and-data-flow-analysis, control-flow-graph, expressions-1, conditional-expressions -> paragraph 1 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: check if-expressions must have both branches.
 * HELPERS: checkType
 */

// TESTCASE NUMBER: 1

fun case1() {
    val b = true
    val a = if (b) {
        "true"
    } else {
        "false"
    }
    a checkType { check<String>() }

    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>a<!>
}
// TESTCASE NUMBER: 2

fun case2() {
    val b = true
    val a = if (b) "true" else "false"
    a checkType { check<String>() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>a<!>
}

// TESTCASE NUMBER: 3

fun case3() {
    val b = true
    //subcase 1 (ELSE BRANCH)
    val case_3_1 = if (!b) {
        "true"
    } else boo()
    case_3_1 checkType { check<Any>() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>case_3_1<!>

    case_3_1 as kotlin.Unit
    case_3_1 checkType { check<Unit>() }

    //subcase 2 (IF TRUE BRANCH)
    val case_3_2 = if (b) {
        "true"
    } else boo()
    case_3_2 checkType { check<Any>() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any")!>case_3_2<!>
    case_3_2 as kotlin.String
    case_3_2 checkType { check<String>() }
}

fun boo(): Any {
    return kotlin.Unit
}
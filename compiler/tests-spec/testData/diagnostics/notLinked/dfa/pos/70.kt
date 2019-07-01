// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 70
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes
 */

/*
 * TESTCASE NUMBER: 1
 * ISSUES: KT-11727
 */
fun case_1(): Int? {
    val x: Int? = null
    return when (x != null) {
        true -> {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?"), DEBUG_INFO_SMARTCAST!>x<!>.inv()
        }
        else -> null
    }
}

/*
 * TESTCASE NUMBER: 2
 * ISSUES: KT-11727
 */
fun case_2(): Int? {
    val x: Int? = null
    return when (x != null) {
        false -> {
            <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Nothing?")!>x<!>
        }
        else -> null
    }
}

/*
 * TESTCASE NUMBER: 3
 * ISSUES: KT-11727
 */
fun case_3(): Int? {
    val x: Int? = null
    return when (x == null) {
        false -> {
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int & kotlin.Int?")!>x<!>
        }
        else -> null
    }
}

/*
 * TESTCASE NUMBER: 4
 * ISSUES: KT-11727
 */
fun case_4(): Int? {
    val x: Int? = null
    return when (x == null) {
        true -> {
            <!DEBUG_INFO_CONSTANT, DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int? & kotlin.Nothing?")!>x<!>
        }
        else -> null
    }
}

// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * PLACE: expressions, constant-literals, character-literals -> paragraph 4 -> sentence 1
 * RELEVANT PLACES: expressions, constant-literals, character-literals -> paragraph 6 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: to define a character the unicode codepoint escaped symbol \u could be used with followed by exactly four hexadecimal digits.
 * HELPERS: checkType
 */

// TESTCASE NUMBER: 1

fun case1() {
    val cMin = '\u0000'
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>cMin<!>
    cMin checkType { check<Char>()}

    val cMax = '\uffff'
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>cMax<!>
    cMax checkType { check<Char>()}

    val cMax1 = '\uFFFF'
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>cMax1<!>
    cMax1 checkType { check<Char>()}
}


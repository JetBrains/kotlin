// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-218
 * PLACE: expressions, constant-literals, character-literals -> paragraph 1 -> sentence 1
 * RELEVANT PLACES: expressions, constant-literals, character-literals -> paragraph 1 -> sentence 2
 * expressions, constant-literals, character-literals -> paragraph 2 -> sentence 1
 * expressions, constant-literals, character-literals -> paragraph 2 -> sentence 2
 * expressions, constant-literals, character-literals -> paragraph 4 -> sentence 1
 * expressions, constant-literals, character-literals -> paragraph 6 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: A character literal defines a constant holding a unicode character value
 * HELPERS: checkType
 */

// TESTCASE NUMBER: 1
fun case1() {
    val c = ' ' //u+0020
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>c<!>
    c checkType { check<Char>()}

    val cMax = '￿' //u+ffff
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Char")!>cMax<!>
    cMax checkType { check<Char>()}
}


// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-218
 * PLACE: expressions, constant-literals, character-literals -> paragraph 1 -> sentence 1
 * RELEVANT PLACES: expressions, constant-literals, character-literals -> paragraph 1 -> sentence 2
 * expressions, constant-literals, character-literals -> paragraph 2 -> sentence 1
 * expressions, constant-literals, character-literals -> paragraph 2 -> sentence 2
 * expressions, constant-literals, character-literals -> paragraph 4 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: A character literal defines a constant holding a unicode character value
 * HELPERS: checkType
 */

// TESTCASE NUMBER: 1
fun case1() {
    val c = <!ILLEGAL_CONST_EXPRESSION!>''<!>
}

// TESTCASE NUMBER: 2

fun case2() {
    val c2: Char = <!ILLEGAL_CONST_EXPRESSION!>''<!><!SYNTAX!>'<!>
    val c3: Char = <!ILLEGAL_CONST_EXPRESSION!>'\'<!>
}

// TESTCASE NUMBER: 3

fun case3() {
    val c1: Char = <!ILLEGAL_CONST_EXPRESSION!>'B a'<!>
    val c2: Char = <!ILLEGAL_CONST_EXPRESSION!>'  '<!>
    val c3: Char = <!ILLEGAL_CONST_EXPRESSION!>'Ba'<!>
}

// TESTCASE NUMBER: 4

fun case4() {
    val cOutOfRaneMin = <!ILLEGAL_CONST_EXPRESSION!>'𐀀'<!> //u+10000

    val cOutOfRangeAroundMax = <!ILLEGAL_CONST_EXPRESSION!>'󠇿󠇿󟿿'<!> //u+Dfffff
}

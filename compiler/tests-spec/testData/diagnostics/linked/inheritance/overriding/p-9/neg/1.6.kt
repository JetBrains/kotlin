// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-591
 * MAIN LINK: inheritance, overriding -> paragraph 9 -> sentence 1
 * PRIMARY LINKS: inheritance, overriding -> paragraph 7 -> sentence 1
 * inheritance, overriding -> paragraph 2 -> sentence 1
 * inheritance, overriding -> paragraph 2 -> sentence 2
 * inheritance, overriding -> paragraph 4 -> sentence 1
 * declarations, classifier-declaration, data-class-declaration -> paragraph 6 -> sentence 1
 * SECONDARY LINKS: declarations, classifier-declaration, data-class-declaration -> paragraph 5 -> sentence 1
 * NUMBER: 6
 * DESCRIPTION: data class functions overriding without override modifier
 */


// TESTCASE NUMBER: 1
data class Case1(val a: Int, val b: CharSequence) {
    fun <!VIRTUAL_MEMBER_HIDDEN!>toString<!>(): String = TODO() //(1)
}

// TESTCASE NUMBER: 2
data class Case2(val a: Int, val b: CharSequence) {
    fun <!VIRTUAL_MEMBER_HIDDEN!>equals<!>(other: Any?): Boolean = TODO() //(0)
}

// TESTCASE NUMBER: 3
data class Case3(val a: Int, val b: CharSequence) {
    fun <!VIRTUAL_MEMBER_HIDDEN!>hashCode<!>(): Int = TODO() //(0)
}


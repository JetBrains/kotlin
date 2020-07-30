// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-591
 * MAIN LINK: inheritance, overriding -> paragraph 5 -> sentence 1
 * PRIMARY LINKS: declarations, classifier-declaration, data-class-declaration -> paragraph 6 -> sentence 1
 * declarations, classifier-declaration, data-class-declaration -> paragraph 6 -> sentence 2
 * SECONDARY LINKS: declarations, classifier-declaration, data-class-declaration -> paragraph 5 -> sentence 1
 * NUMBER: 6
 * DESCRIPTION: overriding member toString function of data class
 */

// TESTCASE NUMBER: 1
data class Case1(val a: Int, val b: CharSequence) {
    override fun toString(): String = TODO()
}

fun case1(b: Case1) {
    b.<!DEBUG_INFO_CALL("fqName: Case1.toString; typeCall: function")!>toString()<!>
}

// TESTCASE NUMBER: 2
data class Case2(val a: Int, val b: CharSequence) {
    override fun equals(other: Any?): Boolean = TODO()
}

fun case2(b: Case2) {
    b.<!DEBUG_INFO_CALL("fqName: Case2.equals; typeCall: operator function")!>equals("")<!>
}


// TESTCASE NUMBER: 1
data class Case3(val a: Int, val b: CharSequence) {
    override fun hashCode(): Int = TODO()
}

fun case1(b: Case3) {
    b.<!DEBUG_INFO_CALL("fqName: Case3.hashCode; typeCall: function")!>hashCode()<!>
}

// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNREACHABLE_CODE -UNUSED_PARAMETER
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: declarations, classifier-declaration, data-class-declaration -> paragraph 6 -> sentence 3
 * SECONDARY LINKS: declarations, classifier-declaration, data-class-declaration -> paragraph 5 -> sentence 1
 * NUMBER: 5
 * DESCRIPTION: copy function cannot be explicified
 */

// TESTCASE NUMBER: 1
data class <!CONFLICTING_JVM_DECLARATIONS, CONFLICTING_JVM_DECLARATIONS!>Case1(val a: Int, val b: String)<!> {
    <!CONFLICTING_OVERLOADS!>fun copy(a: Int = this.a, b: String = this.b): Case1<!> {
        return TODO()
    }
}

// TESTCASE NUMBER: 2
data class Case2(val a: Int, val b: String) {
    <!CONFLICTING_OVERLOADS!>@JvmName("boo")
    fun copy(a: Int = this.a, b: String = this.b): Case1<!> {
        return TODO()
    }
}
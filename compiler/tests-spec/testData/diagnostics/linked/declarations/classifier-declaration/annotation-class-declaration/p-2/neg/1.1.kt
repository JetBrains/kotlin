// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: declarations, classifier-declaration, annotation-class-declaration -> paragraph 2 -> sentence 1
 * NUMBER: 1
 * DESCRIPTION: annotation class cannot have any secondary constructors
 */

// TESTCASE NUMBER: 1
annotation class Case1(val why: String) {
    <!ANNOTATION_CLASS_MEMBER, PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED!>constructor()<!>
}

// TESTCASE NUMBER: 2
annotation class Case2(val why: String) {
    <!ANNOTATION_CLASS_MEMBER!>constructor() : this("")<!>
}
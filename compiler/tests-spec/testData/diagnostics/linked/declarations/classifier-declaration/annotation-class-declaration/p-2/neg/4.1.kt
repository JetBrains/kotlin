// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: declarations, classifier-declaration, annotation-class-declaration -> paragraph 2 -> sentence 4
 * NUMBER: 1
 * DESCRIPTION: annotation class cannot implement interfaces
 */

// TESTCASE NUMBER: 1
annotation class Case1() : <!SUPERTYPES_FOR_ANNOTATION_CLASS!>I<!>

interface I
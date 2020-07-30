// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-591
 * MAIN LINK: declarations, classifier-declaration, data-class-declaration -> paragraph 1 -> sentence 2
 * NUMBER: 2
 * DESCRIPTION: Non-property constructor parameters with default value in the primary constructor
 */

// TESTCASE NUMBER: 1
data class A(val x: Nothing, <!DATA_CLASS_NOT_PROPERTY_PARAMETER!>y: Nothing = TODO()<!>)

// TESTCASE NUMBER: 2
data class B(val x: Any, <!DATA_CLASS_NOT_PROPERTY_PARAMETER!>y: Any = 1<!>)

// TESTCASE NUMBER: 3
data class C(val x: Any = TODO(), <!DATA_CLASS_NOT_PROPERTY_PARAMETER!>y: Any = 1<!>)

// TESTCASE NUMBER: 4
data class D(val x: Any = TODO(), <!DATA_CLASS_NOT_PROPERTY_PARAMETER!>y: Any = 1<!>)

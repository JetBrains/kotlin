// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -REDUNDANT_PROJECTION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: declarations, classifier-declaration, data-class-declaration -> paragraph 1 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: Non-property constructor parameters in the primary constructor
 */

// TESTCASE NUMBER: 1
data class A(val x: Int, <!DATA_CLASS_NOT_PROPERTY_PARAMETER!><!UNUSED_PARAMETER!>y<!>: Int<!>)

// TESTCASE NUMBER: 2
data class B<T>(val x: T, <!DATA_CLASS_NOT_PROPERTY_PARAMETER!><!UNUSED_PARAMETER!>y<!>: T<!>)

// TESTCASE NUMBER: 3
data class C<T>(val x: T, <!DATA_CLASS_NOT_PROPERTY_PARAMETER!><!UNUSED_PARAMETER!>y<!>: List<out T><!>)

// TESTCASE NUMBER: 4
data class D<T>(<!DATA_CLASS_NOT_PROPERTY_PARAMETER!><!UNUSED_PARAMETER!>x<!>: T<!>, val y: List<out T>)

// TESTCASE NUMBER: 5
data class E(val x: Int, <!DATA_CLASS_NOT_PROPERTY_PARAMETER, DATA_CLASS_VARARG_PARAMETER!>vararg <!UNUSED_PARAMETER!>y<!>: Int<!>)

// TESTCASE NUMBER: 6
data class F<T>(val x: T, <!DATA_CLASS_NOT_PROPERTY_PARAMETER, DATA_CLASS_VARARG_PARAMETER!>vararg <!UNUSED_PARAMETER!>y<!>: T<!>)

// TESTCASE NUMBER: 7
data class G<T>(val x: T, <!DATA_CLASS_NOT_PROPERTY_PARAMETER, DATA_CLASS_VARARG_PARAMETER!>vararg <!UNUSED_PARAMETER!>y<!>: List<out T><!>)

// TESTCASE NUMBER: 8
data class H<T>(<!DATA_CLASS_NOT_PROPERTY_PARAMETER!><!UNUSED_PARAMETER!>x<!>: T<!>, <!DATA_CLASS_NOT_PROPERTY_PARAMETER, DATA_CLASS_VARARG_PARAMETER!>vararg <!UNUSED_PARAMETER!>y<!>: List<out T><!>)

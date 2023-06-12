// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-544
 * MAIN LINK: declarations, classifier-declaration, class-declaration, constructor-declaration -> paragraph 5 -> sentence 1
 * PRIMARY LINKS: declarations, classifier-declaration, class-declaration, constructor-declaration -> paragraph 4 -> sentence 1
 * declarations, classifier-declaration, class-declaration -> paragraph 1 -> sentence 1
 * declarations, classifier-declaration, class-declaration, constructor-declaration -> paragraph 2 -> sentence 1
 * type-system, type-contexts-and-scopes, inner-and-nested-type-contexts -> paragraph 1 -> sentence 2
 * declarations, classifier-declaration, data-class-declaration -> paragraph 1 -> sentence 2
 * SECONDARY LINKS: declarations, classifier-declaration, class-declaration -> paragraph 2 -> sentence 2
 * NUMBER: 2
 * DESCRIPTION: Primary constructor for nested class with regular constructor parameter
 */

// TESTCASE NUMBER: 1
data class A(<!DATA_CLASS_NOT_PROPERTY_PARAMETER!>x: Any?<!>)
data class B(<!DATA_CLASS_NOT_PROPERTY_PARAMETER!>x: Any<!>)
data class C(<!DATA_CLASS_NOT_PROPERTY_PARAMETER!>c: () -> Any<!>)
data class D(<!DATA_CLASS_NOT_PROPERTY_PARAMETER!>e: Enum<*><!>)
data class E(<!DATA_CLASS_NOT_PROPERTY_PARAMETER!>n: Nothing<!>)
data class F<T>(<!DATA_CLASS_NOT_PROPERTY_PARAMETER!>t: T<!>)


// TESTCASE NUMBER: 2
class Case2<T>() {
    data class A(<!DATA_CLASS_NOT_PROPERTY_PARAMETER!>t: <!UNRESOLVED_REFERENCE!>T<!><!>)
    data class B(<!DATA_CLASS_NOT_PROPERTY_PARAMETER!>x: List<<!UNRESOLVED_REFERENCE!>T<!>><!>)
    data class C(<!DATA_CLASS_NOT_PROPERTY_PARAMETER!>c: () -> <!UNRESOLVED_REFERENCE!>T<!><!>)
    data class E(<!DATA_CLASS_NOT_PROPERTY_PARAMETER!>n: Nothing<!>, <!DATA_CLASS_NOT_PROPERTY_PARAMETER!>t: <!UNRESOLVED_REFERENCE!>T<!><!>)
}
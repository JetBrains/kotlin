// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-544
 * MAIN LINK: declarations, classifier-declaration, class-declaration, constructor-declaration -> paragraph 5 -> sentence 3
 * PRIMARY LINKS: declarations, classifier-declaration, class-declaration, constructor-declaration -> paragraph 4 -> sentence 1
 * declarations, classifier-declaration, class-declaration -> paragraph 1 -> sentence 1
 * declarations, classifier-declaration, class-declaration, constructor-declaration -> paragraph 2 -> sentence 1
 * type-system, type-contexts-and-scopes, inner-and-nested-type-contexts -> paragraph 1 -> sentence 2
 * SECONDARY LINKS: declarations, classifier-declaration, class-declaration -> paragraph 2 -> sentence 2
 * NUMBER: 1
 * DESCRIPTION: Primary constructor for nested class with mutable property constructor parameter
 */

// TESTCASE NUMBER: 1
class Case1<T>() {
    class A(var t: <!UNRESOLVED_REFERENCE!>T<!>)
    class B(var x: List<<!UNRESOLVED_REFERENCE!>T<!>>)
    class C(var c: () -> <!UNRESOLVED_REFERENCE!>T<!>)
    class E(var n: Nothing, var t: <!UNRESOLVED_REFERENCE!>T<!>)
}
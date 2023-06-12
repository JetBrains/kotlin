// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-544
 * MAIN LINK: declarations, classifier-declaration, class-declaration, constructor-declaration -> paragraph 5 -> sentence 3
 * PRIMARY LINKS: declarations, classifier-declaration, class-declaration, constructor-declaration -> paragraph 4 -> sentence 1
 * declarations, classifier-declaration, class-declaration -> paragraph 1 -> sentence 1
 * declarations, classifier-declaration, class-declaration, constructor-declaration -> paragraph 2 -> sentence 1
 * SECONDARY LINKS: declarations, classifier-declaration, class-declaration -> paragraph 2 -> sentence 2
 * declarations, classifier-declaration, class-declaration -> paragraph 2 -> sentence 6
 * NUMBER: 3
 * DESCRIPTION: Primary constructor for inner class with mutable property constructor parameter
 */

// TESTCASE NUMBER: 1
class Case1 {
    inner class A(var x: Any?)
    inner class B(var x: Any)
    inner class C(var c: () -> Any)
    inner class D(var e: Enum<*>)
    inner class E(var n: Nothing)
    inner class F<T>(var t: T)
}

// TESTCASE NUMBER: 2
class Case2<T>() {
    inner class A(var x: Any?, t: T)
    inner class B(var x: T)
    inner class C(var c: () -> T)
    inner class D<T : Enum<*>>(var e: T)
    inner class E(var n: Nothing, var t: T)
    inner class F<T>(var t: T)
}


// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-544
 * MAIN LINK: declarations, classifier-declaration, class-declaration, constructor-declaration -> paragraph 5 -> sentence 3
 * PRIMARY LINKS: declarations, classifier-declaration, class-declaration, constructor-declaration -> paragraph 4 -> sentence 1
 * declarations, classifier-declaration, class-declaration -> paragraph 1 -> sentence 1
 * declarations, classifier-declaration, class-declaration, constructor-declaration -> paragraph 2 -> sentence 1
 * declarations, classifier-declaration, class-declaration, nested-and-inner-classifiers -> paragraph 1 -> sentence 1
 * SECONDARY LINKS: declarations, classifier-declaration, class-declaration -> paragraph 2 -> sentence 2
 * declarations, classifier-declaration, class-declaration -> paragraph 2 -> sentence 6
 * NUMBER: 4
 * DESCRIPTION: Primary constructor for nested class with mutable property constructor parameter
 */

// TESTCASE NUMBER: 1
fun <T> List<T>.case1() {
    class Case1(var t: T)
    class A(var t: T)
    class B(var x: List<T>)
    class C(var c: () -> T)
    class E(var n: Nothing, var t: T)
}

// TESTCASE NUMBER: 2
val <T> List<T>.case2: Int
    get() = {
        class A(var t: T)
        class B(var x: List<T>)
        class C(var c: () -> T)
        class E(var n: Nothing=TODO(), var t: T)

        fun test() {
            A(this.first())
            B(this)
            C { this.last() }
            E(t = this[2])
        }

        1
    }()

// TESTCASE NUMBER: 3
var <T> List<T>.case3: Unit
    get() {
        class A(var t: T)
        class B(var x: List<T>)
        class C(var c: () -> T)
        class E(var n: Nothing = TODO(), t: T)

        fun test() {
            A(this.first())
            B(this)
            C { this.last() }
            E(t = this[2])
        }
    }
    set(i: Unit) {
        class A(var t: T)
        class B(var x: List<T>)
        class C(var c: () -> T)
        class E( t: T, var n: Nothing =TODO())

        fun test() {
            A(this.first())
            B(this)
            C { this.last() }
            E(t = this[2])
        }
    }


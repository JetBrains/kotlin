// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE
/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-152
 * PRIMARY LINKS: expressions, when-expression -> paragraph 5 -> sentence 1
 * expressions, when-expression -> paragraph 6 -> sentence 1
 * expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 6
 * expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 7
 * expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 8
 */

sealed class A {
    class B: A() {
        class C: A()
    }
}

class D: A()

fun test(a: A) {
    val nonExhaustive = <!NO_ELSE_IN_WHEN!>when<!> (a) {
        is A.B -> "B"
        is A.B.C -> "C"
    }

    val exhaustive = when (a) {
        is A.B -> "B"
        is A.B.C -> "C"
        is D -> "D"
    }
}

// !DIAGNOSTICS: -UNUSED_VARIABLE

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
// FIR_IDENTICAL
// KT-58897

enum class A {
    A,
    B,
}

fun test2(a: A) = when (a) {
    A.A -> "A"
    A.B -> "B"
}

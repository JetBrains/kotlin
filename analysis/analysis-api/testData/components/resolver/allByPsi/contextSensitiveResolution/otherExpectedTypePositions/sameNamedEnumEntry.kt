// ISSUE: KT-58939
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

enum class A {
    A,
    B,
}

fun test(a: A) {
    when (a) {
        A.A -> "A"
        A.B -> "B"
    }
}

// LANGUAGE: +ExpectedTypeGuidedResolution
// KT-58939

enum class A {
    A,
    B,
}

fun test(a: A) = when (a) {
    A.A -> "A"
    A.B -> "B"
}

// LANGUAGE: +ContextSensitiveEnumResolutionInWhen
// KT-58939

enum class A {
    A,
    B,
}

fun test(a: A) = <!NO_ELSE_IN_WHEN!>when<!> (a) {
    A.<!UNRESOLVED_REFERENCE!>A<!> -> "A"
    A.<!UNRESOLVED_REFERENCE!>B<!> -> "B"
}

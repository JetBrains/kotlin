// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses
// FILE: a.kt
fun box() = A(0).f()

// FILE: b.kt
OPTIONAL_JVM_INLINE_ANNOTATION
value class A(val i: Int)

fun A.f(xs: Array<String> = Array<String>(1) { "OK" }) = xs[i]

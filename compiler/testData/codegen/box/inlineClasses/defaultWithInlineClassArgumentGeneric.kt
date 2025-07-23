// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter
// FILE: b.kt
OPTIONAL_JVM_INLINE_ANNOTATION
value class A<T: Int>(val i: T)

fun <T: Int> A<T>.f(xs: Array<String> = Array<String>(1) { "OK" }) = xs[i]

// FILE: a.kt
fun box() = A(0).f()

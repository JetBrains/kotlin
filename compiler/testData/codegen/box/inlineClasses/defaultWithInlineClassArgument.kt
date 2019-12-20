// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR
// FILE: a.kt
fun box() = A(0).f()

// FILE: b.kt
inline class A(val i: Int)

fun A.f(xs: Array<String> = Array<String>(1) { "OK" }) = xs[i]

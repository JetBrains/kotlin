// WITH_STDLIB
// FILE: a.kt
fun box() = A(0).f()

// FILE: b.kt
@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class A(val i: Int)

fun A.f(xs: Array<String> = Array<String>(1) { "OK" }) = xs[i]

// See KT-49659
// WITH_STDLIB
// MODULE: lib
// FILE: lib.kt
@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class A(val value: String)

fun interface B {
    fun f(a: A): String
}

// MODULE: main(lib)
// FILE: test.kt
fun get(b: B) = b.f(A("OK"))

fun box(): String {
    val l = { a: A -> a.value }
    return get(l)
}

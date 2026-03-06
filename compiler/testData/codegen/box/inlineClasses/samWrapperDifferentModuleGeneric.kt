// See KT-49659
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

// MODULE: lib
// FILE: lib.kt

OPTIONAL_JVM_INLINE_ANNOTATION
value class A<T: String>(val value: T)

fun interface B {
    fun f(a: A<String>): String
}

// MODULE: main(lib)
// FILE: test.kt
fun get(b: B) = b.f(A("OK"))

fun box(): String {
    val l = { a: A<String> -> a.value }
    return get(l)
}

// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

// FILE: lib.kt
inline fun runInline(fn: () -> String) = fn()

// FILE: main.kt
OPTIONAL_JVM_INLINE_ANNOTATION
value class R<T: Int>(private val r: T) {
    fun test() = runInline { "OK" }
}

fun box() = R(0).test()
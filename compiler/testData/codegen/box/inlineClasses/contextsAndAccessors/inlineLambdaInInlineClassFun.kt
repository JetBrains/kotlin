// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

// FILE: lib.kt
inline fun runInline(fn: () -> String) = fn()

// FILE: main.kt
OPTIONAL_JVM_INLINE_ANNOTATION
value class R(private val r: Int) {
    fun test() = runInline { "OK" }
}

fun box() = R(0).test()
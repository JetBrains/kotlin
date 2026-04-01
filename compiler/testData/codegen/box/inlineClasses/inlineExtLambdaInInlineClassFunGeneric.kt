// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +JvmInlineMultiFieldValueClasses, +GenericInlineClassParameter

// FILE: lib.kt
inline fun <T> T.runInlineExt(fn: T.() -> String) = fn()

// FILE: main.kt
OPTIONAL_JVM_INLINE_ANNOTATION
value class R<T: Int>(private val r: T) {
    fun test() = runInlineExt { "OK" }
}

fun box() = R(0).test()
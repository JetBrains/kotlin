// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

// FILE: lib.kt
inline fun <T> T.runInlineExt(fn: T.() -> String) = fn()

// FILE: main.kt
OPTIONAL_JVM_INLINE_ANNOTATION
value class R<T: String>(private val r: T) {
    fun test() = runInlineExt { r }
}

fun box() = R("OK").test()
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +JvmInlineMultiFieldValueClasses

// FILE: lib.kt
inline fun <T> T.runInlineExt(fn: T.() -> String) = fn()

// FILE: main.kt
OPTIONAL_JVM_INLINE_ANNOTATION
value class R(private val r: String) {
    fun test() = runInlineExt { r }
}

fun box() = R("OK").test()
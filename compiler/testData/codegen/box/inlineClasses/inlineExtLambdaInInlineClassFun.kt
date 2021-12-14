// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

inline fun <T> T.runInlineExt(fn: T.() -> String) = fn()

OPTIONAL_JVM_INLINE_ANNOTATION
value class R(private val r: Int) {
    fun test() = runInlineExt { "OK" }
}

fun box() = R(0).test()
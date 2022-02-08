// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

fun <T> T.runExt(fn: T.() -> String) = fn()

OPTIONAL_JVM_INLINE_ANNOTATION
value class R(private val r: String) {
    fun test() = runExt { r }
}

fun box() = R("OK").test()
// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

inline fun <T> T.runInlineExt(fn: T.() -> String) = fn()

inline class R(private val r: Int) {
    fun test() = runInlineExt { "OK" }
}

fun box() = R(0).test()
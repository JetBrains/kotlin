// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

inline fun runInline(fn: () -> String) = fn()

inline class R(private val r: Int) {
    fun test() = runInline { "OK" }
}

fun box() = R(0).test()
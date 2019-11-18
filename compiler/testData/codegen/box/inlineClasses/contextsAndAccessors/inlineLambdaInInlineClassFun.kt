// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR

inline fun runInline(fn: () -> String) = fn()

inline class R(private val r: Int) {
    fun test() = runInline { "OK" }
}

fun box() = R(0).test()
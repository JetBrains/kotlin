// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

fun <T> T.runExt(fn: T.() -> String) = fn()

inline class R(private val r: Int) {
    fun test() = runExt { "OK" }
}

fun box() = R(0).test()
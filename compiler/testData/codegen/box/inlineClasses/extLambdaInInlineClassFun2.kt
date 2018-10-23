// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

fun <T> T.runExt(fn: T.() -> String) = fn()

inline class R(private val r: String) {
    fun test() = runExt { r }
}

fun box() = R("OK").test()
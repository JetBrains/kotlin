// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

inline class R(private val r: Int) {
    fun test() = { ok() }()

    fun ok() = "OK"
}

fun box() = R(0).test()
// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

interface Base {
    fun result(): Int
}

inline class Inlined(val x: Int) : Base {
    override fun result(): Int = x
}

fun foo(b: Boolean): Base {
    return if (b) Inlined(0) else Inlined(1)
}

fun box(): String {
    if (foo(true).result() != 0) return "Fail 1"
    if (foo(false).result() != 1) return "Fail 2"
    return "OK"
}
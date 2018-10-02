// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

inline class Z(val x: Int) {
    constructor(x: Long = 42L) : this(x.toInt())
}

fun box(): String {
    if (Z().x != 42) throw AssertionError()

    return "OK"
}
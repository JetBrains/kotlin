// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR

inline class Z(val x: Int) {
    fun test(y: Int = 42) = x + y
}

fun box(): String {
    if (Z(800).test() != 842) throw AssertionError()
    if (Z(400).test(32) != 432) throw AssertionError()

    return "OK"
}
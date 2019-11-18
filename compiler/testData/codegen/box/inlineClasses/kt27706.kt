// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

inline class Z(val x: Int) {
    inner class Inner(val z: Z) {
        val xx = x
    }
}

fun box(): String {
    val zi = Z(42).Inner(Z(100))
    if (zi.xx != 42) throw AssertionError()
    if (zi.z.x != 100) throw AssertionError()

    return "OK"
}
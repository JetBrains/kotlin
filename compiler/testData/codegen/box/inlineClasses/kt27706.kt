// !LANGUAGE: +InlineClasses
// WITH_RUNTIME

inline class Z(val x: Int) {
    @Suppress("INNER_CLASS_INSIDE_INLINE_CLASS")
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
// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Z(val x: Int) {
    @Suppress("INNER_CLASS_INSIDE_VALUE_CLASS")
    inner class Inner(val y: Int) {
        val xx = x
    }
}

fun box(): String {
    val zi = Z(42).Inner(100)
    if (zi.xx != 42) throw AssertionError()
    if (zi.y != 100) throw AssertionError()

    return "OK"
}
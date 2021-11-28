// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Z(val x: Int) {
    fun test(y: Int = 42) = x + y
}

fun box(): String {
    if (Z(800).test() != 842) throw AssertionError()
    if (Z(400).test(32) != 432) throw AssertionError()

    return "OK"
}
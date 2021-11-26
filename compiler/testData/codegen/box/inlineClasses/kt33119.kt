// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class WrappingInt(val value: Int) {
    operator fun inc(): WrappingInt = plus(1)
    operator fun plus(num: Int): WrappingInt = WrappingInt((value + num) and 0xFFFF)
}

fun box(): String {
    var x = WrappingInt(65535)
    x++
    if (x.value != 0) throw AssertionError("x++: ${x.value}")

    var y = WrappingInt(65535)
    ++y
    if (y.value != 0) throw AssertionError("++y: ${y.value}")

    return "OK"
}
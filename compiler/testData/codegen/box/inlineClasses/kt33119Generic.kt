// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class WrappingInt<T: Int>(val value: T) {
    operator fun inc(): WrappingInt<T> = plus(1)
    operator fun plus(num: Int): WrappingInt<T> = WrappingInt(((value + num) and 0xFFFF) as T)
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
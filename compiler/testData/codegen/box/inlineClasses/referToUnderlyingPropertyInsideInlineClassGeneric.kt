// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class UInt<T: Int>(val value: T) {
    operator fun plus(other: UInt<T>) = UInt(value + other.value)
    fun otherValue(other: UInt<T>) = other.value
}

fun box(): String {
    val a = UInt(10)
    val b = UInt(20)
    if (a.otherValue(b) != 20) return "fail 1"

    if ((a + b).value != 30) return "fail 2"

    return "OK"
}
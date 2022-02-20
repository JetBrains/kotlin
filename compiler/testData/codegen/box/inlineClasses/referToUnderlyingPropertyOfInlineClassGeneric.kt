// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class UInt<T: Int>(val value: T)

fun box(): String {
    val a = UInt(123)
    if(a.value != 123) return "fail"

    val c = a.value.hashCode()
    if (c.hashCode() != 123.hashCode()) return "fail"

    val b = UInt(100).value + a.value
    if (b != 223) return "faile"

    return "OK"
}
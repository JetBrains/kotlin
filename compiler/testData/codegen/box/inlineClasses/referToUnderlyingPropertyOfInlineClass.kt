// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class UInt(val value: Int)

fun box(): String {
    val a = UInt(123)
    if(a.value != 123) return "fail"

    val c = a.value.hashCode()
    if (c.hashCode() != 123.hashCode()) return "fail"

    val b = UInt(100).value + a.value
    if (b != 223) return "faile"

    return "OK"
}
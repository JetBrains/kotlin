// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class UInt<T: Int>(val value: T)

fun <T> last(vararg e: T): T = e[e.size - 1]
fun <T> first(vararg e: T): T = e[0]

fun box(): String {
    val u0 = UInt(0)
    val us = arrayOf(UInt(1), UInt(2), UInt(3))

    if (last(u0, *us).value != 3) return "fail"
    if (first(*us, u0).value != 1) return "fail"
    if (first(u0, *us).value != 0) return "fail"

    return "OK"
}
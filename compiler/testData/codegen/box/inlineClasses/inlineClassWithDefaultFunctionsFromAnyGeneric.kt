// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z<T: Int>(val data: T)

fun box(): String {
    if (Z(0) != Z(0)) throw AssertionError()
    if (Z(0) == Z(1)) throw AssertionError()

    if (Z(1234).hashCode() != 1234) throw AssertionError(Z(1234).hashCode().toString())

    if (Z(0).toString() != "Z(data=0)") throw AssertionError(Z(0).toString())

    return "OK"
}
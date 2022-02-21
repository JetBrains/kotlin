// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class NullableInt<T: Any>(private val holder: T?) {
    val intValue: Int get() = holder as Int
}

val prop: ArrayList<NullableInt<Any>> = arrayListOf(NullableInt(0))

fun box(): String {
    val a = prop[0].intValue
    if (a != 0) return "Error 1: $a"

    val local = mutableListOf(NullableInt(1))
    val b = local[0].intValue
    if (b != 1) return "Error 2: $b"

    prop[0] = NullableInt(2)
    if (prop[0].intValue != 2) return "Error 3: ${prop[0].intValue}"

    return "OK"
}

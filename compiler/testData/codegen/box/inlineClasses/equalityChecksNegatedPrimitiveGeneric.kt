// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class A<T: Int>(val x: T)

fun <T: Int> isNotNullVacuousLeft(s: A<T>) = s != null
fun <T: Int> isNotNullVacuousRight(s: A<T>) = null != s
fun <T: Int> isNotNullLeft(s: A<T>?) = s != null
fun <T: Int> isNotNullRight(s: A<T>?) = null != s
fun <T: Int> isNotEqualSame(s: A<T>, t: A<T>) = s != t
fun <T: Int> isNotEqualAnyLeft(s: A<T>, t: Any?) = s != t
fun <T: Int> isNotEqualAnyRight(s: Any?, t: A<T>) = s != t
fun <T: Int> isNotEqualSameNullable(s: A<T>?, t: A<T>?) = s != t
fun <T: Int> isNotEqualAnyNullableLeft(s: A<T>?, t: Any?) = s != t
fun <T: Int> isNotEqualAnyNullableRight(s: Any?, t: A<T>?) = s != t
fun <T: Int> isNotEqualNullableUnboxedLeft(s: A<T>, t: A<T>?) = s != t
fun <T: Int> isNotEqualNullableUnboxedRight(s: A<T>?, t: A<T>) = s != t

fun box(): String {
    if (!isNotNullVacuousLeft(A(0))) return "Fail 1"
    if (!isNotNullVacuousRight(A(0))) return "Fail 2"
    if (!isNotNullLeft(A(0))) return "Fail 3"
    if (!isNotNullRight(A(0))) return "Fail 4"
    if (isNotNullLeft<Int>(null)) return "Fail 5"
    if (isNotNullRight<Int>(null)) return "Fail 6"
    if (isNotEqualSame(A(0), A(0))) return "Fail 7"
    if (!isNotEqualSame(A(0), A(1))) return "Fail 8"
    if (!isNotEqualAnyLeft(A(0), 0)) return "Fail 9"
    if (!isNotEqualAnyLeft(A(0), null)) return "Fail 10"
    if (isNotEqualAnyLeft(A(0), A(0))) return "Fail 11"
    if (!isNotEqualAnyRight(0, A(0))) return "Fail 12"
    if (!isNotEqualAnyRight(null, A(0))) return "Fail 13"
    if (isNotEqualAnyRight(A(0), A(0))) return "Fail 14"
    if (isNotEqualSameNullable<Int>(null, null)) return "Fail 15"
    if (isNotEqualSameNullable(A(0), A(0))) return "Fail 16"
    if (!isNotEqualSameNullable(null, A(0))) return "Fail 17"
    if (!isNotEqualSameNullable(A(0), null)) return "Fail 18"
    if (!isNotEqualSameNullable(A(0), A(1))) return "Fail 19"
    if (isNotEqualAnyNullableLeft<Int>(null, null)) return "Fail 20"
    if (isNotEqualAnyNullableLeft(A(0), A(0))) return "Fail 21"
    if (!isNotEqualAnyNullableLeft(A(0), 0)) return "Fail 22"
    if (!isNotEqualAnyNullableLeft<Int>(null, 0)) return "Fail 23"
    if (!isNotEqualAnyNullableLeft(A(0), null)) return "Fail 24"
    if (!isNotEqualAnyNullableLeft(A(0), A(1))) return "Fail 25"
    if (isNotEqualAnyNullableRight<Int>(null, null)) return "Fail 26"
    if (isNotEqualAnyNullableRight(A(0), A(0))) return "Fail 27"
    if (!isNotEqualAnyNullableRight(0, A(0))) return "Fail 28"
    if (!isNotEqualAnyNullableRight<Int>(0, null)) return "Fail 29"
    if (!isNotEqualAnyNullableRight(null, A(0))) return "Fail 30"
    if (!isNotEqualAnyNullableRight(A(0), A(1))) return "Fail 31"
    if (!isNotEqualNullableUnboxedLeft(A(0), A(1))) return "Fail 32"
    if (isNotEqualNullableUnboxedLeft(A(0), A(0))) return "Fail 33"
    if (!isNotEqualNullableUnboxedRight(A(0), A(1))) return "Fail 34"
    if (isNotEqualNullableUnboxedRight(A(1), A(1))) return "Fail 35"
    if (!isNotEqualNullableUnboxedLeft(A(0), null)) return "Fail 36"
    if (!isNotEqualNullableUnboxedRight(null, A(1))) return "Fail 37"
    return "OK"
}

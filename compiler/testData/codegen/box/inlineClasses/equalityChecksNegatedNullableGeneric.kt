// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// IGNORE_BACKEND: JS_IR
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class A<T>(val x: T)

fun <T> isNotNullVacuousLeft(s: A<T>) = s != null
fun <T> isNotNullVacuousRight(s: A<T>) = null != s
fun <T> isNotNullLeft(s: A<T>?) = s != null
fun <T> isNotNullRight(s: A<T>?) = null != s
fun <T> isNotEqualSame(s: A<T>, t: A<T>) = s != t
fun <T> isNotEqualAnyLeft(s: A<T>, t: Any?) = s != t
fun <T> isNotEqualAnyRight(s: Any?, t: A<T>) = s != t
fun <T> isNotEqualSameNullable(s: A<T>?, t: A<T>?) = s != t
fun <T> isNotEqualAnyNullableLeft(s: A<T>?, t: Any?) = s != t
fun <T> isNotEqualAnyNullableRight(s: Any?, t: A<T>?) = s != t
fun <T> isNotEqualNullableUnboxedLeft(s: A<T>, t: A<T>?) = s != t
fun <T> isNotEqualNullableUnboxedRight(s: A<T>?, t: A<T>) = s != t

fun box(): String {
    if (!isNotNullVacuousLeft(A(0))) return "Fail 1"
    if (!isNotNullVacuousRight(A(0))) return "Fail 2"
    if (!isNotNullLeft(A(0))) return "Fail 3"
    if (!isNotNullRight(A(0))) return "Fail 4"
    if (isNotNullLeft<Any?>(null)) return "Fail 5"
    if (isNotNullRight<Any?>(null)) return "Fail 6"
    if (isNotEqualSame(A(0), A(0))) return "Fail 7"
    if (!isNotEqualSame(A(0), A(1))) return "Fail 8"
    if (!isNotEqualAnyLeft(A(0), 0)) return "Fail 9"
    if (!isNotEqualAnyLeft(A(0), null)) return "Fail 10"
    if (isNotEqualAnyLeft(A(0), A(0))) return "Fail 11"
    if (!isNotEqualAnyRight(0, A(0))) return "Fail 12"
    if (!isNotEqualAnyRight(null, A(0))) return "Fail 13"
    if (isNotEqualAnyRight(A(0), A(0))) return "Fail 14"
    if (isNotEqualSameNullable<Any?>(null, null)) return "Fail 15"
    if (isNotEqualSameNullable(A(0), A(0))) return "Fail 16"
    if (!isNotEqualSameNullable(null, A(0))) return "Fail 17"
    if (!isNotEqualSameNullable(A(0), null)) return "Fail 18"
    if (!isNotEqualSameNullable(A(0), A(1))) return "Fail 19"
    if (isNotEqualAnyNullableLeft<Any?>(null, null)) return "Fail 20"
    if (isNotEqualAnyNullableLeft(A(0), A(0))) return "Fail 21"
    if (!isNotEqualAnyNullableLeft(A(0), 0)) return "Fail 22"
    if (!isNotEqualAnyNullableLeft<Any?>(null, 0)) return "Fail 23"
    if (!isNotEqualAnyNullableLeft(A(0), null)) return "Fail 24"
    if (!isNotEqualAnyNullableLeft(A(0), A(1))) return "Fail 25"
    if (isNotEqualAnyNullableRight<Any?>(null, null)) return "Fail 26"
    if (isNotEqualAnyNullableRight(A(0), A(0))) return "Fail 27"
    if (!isNotEqualAnyNullableRight(0, A(0))) return "Fail 28"
    if (!isNotEqualAnyNullableRight<Any?>(0, null)) return "Fail 29"
    if (!isNotEqualAnyNullableRight(null, A(0))) return "Fail 30"
    if (!isNotEqualAnyNullableRight(A(0), A(1))) return "Fail 31"

    if (!isNotNullVacuousLeft(A(null))) return "Fail 32"
    if (!isNotNullVacuousRight(A(null))) return "Fail 33"
    if (!isNotNullLeft(A(null))) return "Fail 34"
    if (!isNotNullRight(A(null))) return "Fail 35"
    if (!isNotEqualAnyLeft(A(null), null)) return "Fail 36"
    if (!isNotEqualAnyRight(null, A(null))) return "Fail 37"
    if (!isNotEqualAnyNullableLeft(A(null), null)) return "Fail 38"
    if (!isNotEqualAnyNullableRight(null, A(null))) return "Fail 39"
    if (!isNotEqualSameNullable(A(null), null)) return "Fail 42"
    if (!isNotEqualSameNullable(null, A(null))) return "Fail 43"

    if (!isNotEqualNullableUnboxedLeft(A(0), A(1))) return "Fail 44"
    if (isNotEqualNullableUnboxedLeft(A(0), A(0))) return "Fail 45"
    if (!isNotEqualNullableUnboxedRight(A(0), A(1))) return "Fail 46"
    if (isNotEqualNullableUnboxedRight(A(1), A(1))) return "Fail 47"
    if (!isNotEqualNullableUnboxedLeft(A(0), null)) return "Fail 48"
    if (!isNotEqualNullableUnboxedRight(null, A(1))) return "Fail 49"

    if (!isNotEqualNullableUnboxedRight(null, A(null))) return "Fail 50"
    if (!isNotEqualNullableUnboxedLeft(A(null), null)) return "Fail 51"

    return "OK"
}

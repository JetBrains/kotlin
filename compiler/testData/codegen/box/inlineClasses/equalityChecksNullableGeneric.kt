// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// IGNORE_BACKEND: JS_IR
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class A<T>(val x: T)

fun <T> isNullVacuousLeft(s: A<T>) = s == null
fun <T> isNullVacuousRight(s: A<T>) = null == s
fun <T> isNullLeft(s: A<T>?) = s == null
fun <T> isNullRight(s: A<T>?) = null == s
fun <T> isEqualSame(s: A<T>, t: A<T>) = s == t
fun <T> isEqualAnyLeft(s: A<T>, t: Any?) = s == t
fun <T> isEqualAnyRight(s: Any?, t: A<T>) = s == t
fun <T> isEqualSameNullable(s: A<T>?, t: A<T>?) = s == t
fun <T> isEqualAnyNullableLeft(s: A<T>?, t: Any?) = s == t
fun <T> isEqualAnyNullableRight(s: Any?, t: A<T>?) = s == t
fun <T> isEqualNullableUnboxedLeft(s: A<T>, t: A<T>?) = s == t
fun <T> isEqualNullableUnboxedRight(s: A<T>?, t: A<T>) = s == t

fun box(): String {
    if (isNullVacuousLeft(A(0))) return "Fail 1"
    if (isNullVacuousRight(A(0))) return "Fail 2"
    if (isNullLeft(A(0))) return "Fail 3"
    if (isNullRight(A(0))) return "Fail 4"
    if (!isNullLeft<Any?>(null)) return "Fail 5"
    if (!isNullRight<Any?>(null)) return "Fail 6"
    if (!isEqualSame(A(0), A(0))) return "Fail 7"
    if (isEqualSame(A(0), A(1))) return "Fail 8"
    if (isEqualAnyLeft(A(0), 0)) return "Fail 9"
    if (isEqualAnyLeft(A(0), null)) return "Fail 10"
    if (!isEqualAnyLeft(A(0), A(0))) return "Fail 11"
    if (isEqualAnyRight(0, A(0))) return "Fail 12"
    if (isEqualAnyRight(null, A(0))) return "Fail 13"
    if (!isEqualAnyRight(A(0), A(0))) return "Fail 14"
    if (!isEqualSameNullable<Any?>(null, null)) return "Fail 15"
    if (!isEqualSameNullable(A(0), A(0))) return "Fail 16"
    if (isEqualSameNullable(null, A(0))) return "Fail 17"
    if (isEqualSameNullable(A(0), null)) return "Fail 18"
    if (isEqualSameNullable(A(0), A(1))) return "Fail 19"
    if (!isEqualAnyNullableLeft<Any?>(null, null)) return "Fail 20"
    if (!isEqualAnyNullableLeft(A(0), A(0))) return "Fail 21"
    if (isEqualAnyNullableLeft(A(0), 0)) return "Fail 22"
    if (isEqualAnyNullableLeft<Any?>(null, 0)) return "Fail 23"
    if (isEqualAnyNullableLeft(A(0), null)) return "Fail 24"
    if (isEqualAnyNullableLeft(A(0), A(1))) return "Fail 25"
    if (!isEqualAnyNullableRight<Any?>(null, null)) return "Fail 26"
    if (!isEqualAnyNullableRight(A(0), A(0))) return "Fail 27"
    if (isEqualAnyNullableRight(0, A(0))) return "Fail 28"
    if (isEqualAnyNullableRight<Any?>(0, null)) return "Fail 29"
    if (isEqualAnyNullableRight(null, A(0))) return "Fail 30"
    if (isEqualAnyNullableRight(A(0), A(1))) return "Fail 31"

    if (isNullVacuousLeft(A(null))) return "Fail 32"
    if (isNullVacuousRight(A(null))) return "Fail 33"
    if (isNullLeft(A(null))) return "Fail 34"
    if (isNullRight(A(null))) return "Fail 35"
    if (isEqualAnyLeft(A(null), null)) return "Fail 36"
    if (isEqualAnyRight(null, A(null))) return "Fail 37"
    if (isEqualAnyNullableLeft(A(null), null)) return "Fail 38"
    if (isEqualAnyNullableRight(null, A(null))) return "Fail 39"
    if (isEqualSameNullable(A(null), null)) return "Fail 42"
    if (isEqualSameNullable(null, A(null))) return "Fail 43"

    if (isEqualNullableUnboxedLeft(A(0), A(1))) return "Fail 44"
    if (!isEqualNullableUnboxedLeft(A(0), A(0))) return "Fail 45"
    if (isEqualNullableUnboxedRight(A(0), A(1))) return "Fail 46"
    if (!isEqualNullableUnboxedRight(A(1), A(1))) return "Fail 47"
    if (isEqualNullableUnboxedLeft(A(0), null)) return "Fail 48"
    if (isEqualNullableUnboxedRight(null, A(1))) return "Fail 49"

    if (isEqualNullableUnboxedRight(null, A(null))) return "Fail 50"
    if (isEqualNullableUnboxedLeft(A(null), null)) return "Fail 51"

    return "OK"
}

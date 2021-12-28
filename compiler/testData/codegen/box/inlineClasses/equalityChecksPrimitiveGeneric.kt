// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class A<T: Int>(val x: T)

fun <T: Int> isNullVacuousLeft(s: A<T>) = s == null
fun <T: Int> isNullVacuousRight(s: A<T>) = null == s
fun <T: Int> isNullLeft(s: A<T>?) = s == null
fun <T: Int> isNullRight(s: A<T>?) = null == s
fun <T: Int> isEqualSame(s: A<T>, t: A<T>) = s == t
fun <T: Int> isEqualAnyLeft(s: A<T>, t: Any?) = s == t
fun <T: Int> isEqualAnyRight(s: Any?, t: A<T>) = s == t
fun <T: Int> isEqualSameNullable(s: A<T>?, t: A<T>?) = s == t
fun <T: Int> isEqualAnyNullableLeft(s: A<T>?, t: Any?) = s == t
fun <T: Int> isEqualAnyNullableRight(s: Any?, t: A<T>?) = s == t
fun <T: Int> isEqualNullableUnboxedLeft(s: A<T>, t: A<T>?) = s == t
fun <T: Int> isEqualNullableUnboxedRight(s: A<T>?, t: A<T>) = s == t

fun box(): String {
    if (isNullVacuousLeft(A(0))) return "Fail 1"
    if (isNullVacuousRight(A(0))) return "Fail 2"
    if (isNullLeft(A(0))) return "Fail 3"
    if (isNullRight(A(0))) return "Fail 4"
    if (!isNullLeft<Int>(null)) return "Fail 5"
    if (!isNullRight<Int>(null)) return "Fail 6"
    if (!isEqualSame(A(0), A(0))) return "Fail 7"
    if (isEqualSame(A(0), A(1))) return "Fail 8"
    if (isEqualAnyLeft(A(0), 0)) return "Fail 9"
    if (isEqualAnyLeft(A(0), null)) return "Fail 10"
    if (!isEqualAnyLeft(A(0), A(0))) return "Fail 11"
    if (isEqualAnyRight(0, A(0))) return "Fail 12"
    if (isEqualAnyRight(null, A(0))) return "Fail 13"
    if (!isEqualAnyRight(A(0), A(0))) return "Fail 14"
    if (!isEqualSameNullable<Int>(null, null)) return "Fail 15"
    if (!isEqualSameNullable(A(0), A(0))) return "Fail 16"
    if (isEqualSameNullable(null, A(0))) return "Fail 17"
    if (isEqualSameNullable(A(0), null)) return "Fail 18"
    if (isEqualSameNullable(A(0), A(1))) return "Fail 19"
    if (!isEqualAnyNullableLeft<Int>(null, null)) return "Fail 20"
    if (!isEqualAnyNullableLeft(A(0), A(0))) return "Fail 21"
    if (isEqualAnyNullableLeft(A(0), 0)) return "Fail 22"
    if (isEqualAnyNullableLeft<Int>(null, 0)) return "Fail 23"
    if (isEqualAnyNullableLeft(A(0), null)) return "Fail 24"
    if (isEqualAnyNullableLeft(A(0), A(1))) return "Fail 25"
    if (!isEqualAnyNullableRight<Int>(null, null)) return "Fail 26"
    if (!isEqualAnyNullableRight(A(0), A(0))) return "Fail 27"
    if (isEqualAnyNullableRight(0, A(0))) return "Fail 28"
    if (isEqualAnyNullableRight<Int>(0, null)) return "Fail 29"
    if (isEqualAnyNullableRight(null, A(0))) return "Fail 30"
    if (isEqualAnyNullableRight(A(0), A(1))) return "Fail 31"
    if (isEqualNullableUnboxedLeft(A(0), A(1))) return "Fail 32"
    if (!isEqualNullableUnboxedLeft(A(0), A(0))) return "Fail 33"
    if (isEqualNullableUnboxedRight(A(0), A(1))) return "Fail 34"
    if (!isEqualNullableUnboxedRight(A(1), A(1))) return "Fail 35"
    if (isEqualNullableUnboxedLeft(A(0), null)) return "Fail 36"
    if (isEqualNullableUnboxedRight(null, A(1))) return "Fail 37"
    return "OK"
}

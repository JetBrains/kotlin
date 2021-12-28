// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class A<T: String>(val x: T)

fun <T: String> isNotNullVacuousLeft(s: A<T>) = s != null
fun <T: String> isNotNullVacuousRight(s: A<T>) = null != s
fun <T: String> isNotNullLeft(s: A<T>?) = s != null
fun <T: String> isNotNullRight(s: A<T>?) = null != s
fun <T: String> isNotEqualSame(s: A<T>, t: A<T>) = s != t
fun <T: String> isNotEqualAnyLeft(s: A<T>, t: Any?) = s != t
fun <T: String> isNotEqualAnyRight(s: Any?, t: A<T>) = s != t
fun <T: String> isNotEqualSameNullable(s: A<T>?, t: A<T>?) = s != t
fun <T: String> isNotEqualAnyNullableLeft(s: A<T>?, t: Any?) = s != t
fun <T: String> isNotEqualAnyNullableRight(s: Any?, t: A<T>?) = s != t
fun <T: String> isNotEqualNullableUnboxedLeft(s: A<T>, t: A<T>?) = s != t
fun <T: String> isNotEqualNullableUnboxedRight(s: A<T>?, t: A<T>) = s != t

fun box(): String {
    if (!isNotNullVacuousLeft(A(""))) return "Fail 1"
    if (!isNotNullVacuousRight(A(""))) return "Fail 2"
    if (!isNotNullLeft(A(""))) return "Fail 3"
    if (!isNotNullRight(A(""))) return "Fail 4"
    if (isNotNullLeft<String>(null)) return "Fail 5"
    if (isNotNullRight<String>(null)) return "Fail 6"
    if (isNotEqualSame(A(""), A(""))) return "Fail 7"
    if (!isNotEqualSame(A("a"), A("b"))) return "Fail 8"
    if (!isNotEqualAnyLeft(A(""), "")) return "Fail 9"
    if (!isNotEqualAnyLeft(A(""), null)) return "Fail 10"
    if (isNotEqualAnyLeft(A(""), A(""))) return "Fail 11"
    if (!isNotEqualAnyRight("", A(""))) return "Fail 12"
    if (!isNotEqualAnyRight(null, A(""))) return "Fail 13"
    if (isNotEqualAnyRight(A(""), A(""))) return "Fail 14"
    if (isNotEqualSameNullable<String>(null, null)) return "Fail 15"
    if (isNotEqualSameNullable(A(""), A(""))) return "Fail 16"
    if (!isNotEqualSameNullable(null, A(""))) return "Fail 17"
    if (!isNotEqualSameNullable(A(""), null)) return "Fail 18"
    if (!isNotEqualSameNullable(A(""), A("a"))) return "Fail 19"
    if (isNotEqualAnyNullableLeft<String>(null, null)) return "Fail 20"
    if (isNotEqualAnyNullableLeft(A(""), A(""))) return "Fail 21"
    if (!isNotEqualAnyNullableLeft(A(""), "")) return "Fail 22"
    if (!isNotEqualAnyNullableLeft<String>(null, "")) return "Fail 23"
    if (!isNotEqualAnyNullableLeft(A(""), null)) return "Fail 24"
    if (!isNotEqualAnyNullableLeft(A(""), A("a"))) return "Fail 25"
    if (isNotEqualAnyNullableRight<String>(null, null)) return "Fail 26"
    if (isNotEqualAnyNullableRight(A(""), A(""))) return "Fail 27"
    if (!isNotEqualAnyNullableRight("", A(""))) return "Fail 28"
    if (!isNotEqualAnyNullableRight<String>("", null)) return "Fail 29"
    if (!isNotEqualAnyNullableRight(null, A(""))) return "Fail 30"
    if (!isNotEqualAnyNullableRight(A(""), A("a"))) return "Fail 31"
    if (!isNotEqualNullableUnboxedLeft(A(""), A("a"))) return "Fail 32"
    if (isNotEqualNullableUnboxedLeft(A(""), A(""))) return "Fail 33"
    if (!isNotEqualNullableUnboxedRight(A(""), A("a"))) return "Fail 34"
    if (isNotEqualNullableUnboxedRight(A("a"), A("a"))) return "Fail 35"
    if (!isNotEqualNullableUnboxedLeft(A(""), null)) return "Fail 36"
    if (!isNotEqualNullableUnboxedRight(null, A(""))) return "Fail 37"
    return "OK"
}

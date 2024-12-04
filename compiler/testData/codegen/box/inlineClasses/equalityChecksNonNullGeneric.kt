// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class A<T: String>(val x: T)

fun <T: String> isNullVacuousLeft(s: A<T>) = s == null
fun <T: String> isNullVacuousRight(s: A<T>) = null == s
fun <T: String> isNullLeft(s: A<T>?) = s == null
fun <T: String> isNullRight(s: A<T>?) = null == s
fun <T: String> isEqualSame(s: A<T>, t: A<T>) = s == t
fun <T: String> isEqualAnyLeft(s: A<T>, t: Any?) = s == t
fun <T: String> isEqualAnyRight(s: Any?, t: A<T>) = s == t
fun <T: String> isEqualSameNullable(s: A<T>?, t: A<T>?) = s == t
fun <T: String> isEqualAnyNullableLeft(s: A<T>?, t: Any?) = s == t
fun <T: String> isEqualAnyNullableRight(s: Any?, t: A<T>?) = s == t
fun <T: String> isEqualNullableUnboxedLeft(s: A<T>, t: A<T>?) = s == t
fun <T: String> isEqualNullableUnboxedRight(s: A<T>?, t: A<T>) = s == t

fun box(): String {
    if (isNullVacuousLeft(A(""))) return "Fail 1"
    if (isNullVacuousRight(A(""))) return "Fail 2"
    if (isNullLeft(A(""))) return "Fail 3"
    if (isNullRight(A(""))) return "Fail 4"
    if (!isNullLeft<String>(null)) return "Fail 5"
    if (!isNullRight<String>(null)) return "Fail 6"
    if (!isEqualSame(A(""), A(""))) return "Fail 7"
    if (isEqualSame(A("a"), A("b"))) return "Fail 8"
    if (isEqualAnyLeft(A(""), "")) return "Fail 9"
    if (isEqualAnyLeft(A(""), null)) return "Fail 10"
    if (!isEqualAnyLeft(A(""), A(""))) return "Fail 11"
    if (isEqualAnyRight("", A(""))) return "Fail 12"
    if (isEqualAnyRight(null, A(""))) return "Fail 13"
    if (!isEqualAnyRight(A(""), A(""))) return "Fail 14"
    if (!isEqualSameNullable<String>(null, null)) return "Fail 15"
    if (!isEqualSameNullable(A(""), A(""))) return "Fail 16"
    if (isEqualSameNullable(null, A(""))) return "Fail 17"
    if (isEqualSameNullable(A(""), null)) return "Fail 18"
    if (isEqualSameNullable(A(""), A("a"))) return "Fail 19"
    if (!isEqualAnyNullableLeft<String>(null, null)) return "Fail 20"
    if (!isEqualAnyNullableLeft(A(""), A(""))) return "Fail 21"
    if (isEqualAnyNullableLeft(A(""), "")) return "Fail 22"
    if (isEqualAnyNullableLeft<String>(null, "")) return "Fail 23"
    if (isEqualAnyNullableLeft(A(""), null)) return "Fail 24"
    if (isEqualAnyNullableLeft(A(""), A("a"))) return "Fail 25"
    if (!isEqualAnyNullableRight<String>(null, null)) return "Fail 26"
    if (!isEqualAnyNullableRight(A(""), A(""))) return "Fail 27"
    if (isEqualAnyNullableRight("", A(""))) return "Fail 28"
    if (isEqualAnyNullableRight<String>("", null)) return "Fail 29"
    if (isEqualAnyNullableRight(null, A(""))) return "Fail 30"
    if (isEqualAnyNullableRight(A(""), A("a"))) return "Fail 31"
    if (isEqualNullableUnboxedLeft(A(""), A("a"))) return "Fail 32"
    if (!isEqualNullableUnboxedLeft(A(""), A(""))) return "Fail 33"
    if (isEqualNullableUnboxedRight(A(""), A("a"))) return "Fail 34"
    if (!isEqualNullableUnboxedRight(A("a"), A("a"))) return "Fail 35"
    if (isEqualNullableUnboxedLeft(A(""), null)) return "Fail 36"
    if (isEqualNullableUnboxedRight(null, A(""))) return "Fail 37"
    return "OK"
}

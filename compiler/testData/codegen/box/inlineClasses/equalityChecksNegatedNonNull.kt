// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR

inline class A(val x: String)

fun isNotNullVacuousLeft(s: A) = s != null
fun isNotNullVacuousRight(s: A) = null != s
fun isNotNullLeft(s: A?) = s != null
fun isNotNullRight(s: A?) = null != s
fun isNotEqualSame(s: A, t: A) = s != t
fun isNotEqualAnyLeft(s: A, t: Any?) = s != t
fun isNotEqualAnyRight(s: Any?, t: A) = s != t
fun isNotEqualSameNullable(s: A?, t: A?) = s != t
fun isNotEqualAnyNullableLeft(s: A?, t: Any?) = s != t
fun isNotEqualAnyNullableRight(s: Any?, t: A?) = s != t
fun isNotEqualNullableUnboxedLeft(s: A, t: A?) = s != t
fun isNotEqualNullableUnboxedRight(s: A?, t: A) = s != t

fun box(): String {
    if (!isNotNullVacuousLeft(A(""))) return "Fail 1"
    if (!isNotNullVacuousRight(A(""))) return "Fail 2"
    if (!isNotNullLeft(A(""))) return "Fail 3"
    if (!isNotNullRight(A(""))) return "Fail 4"
    if (isNotNullLeft(null)) return "Fail 5"
    if (isNotNullRight(null)) return "Fail 6"
    if (isNotEqualSame(A(""), A(""))) return "Fail 7"
    if (!isNotEqualSame(A("a"), A("b"))) return "Fail 8"
    if (!isNotEqualAnyLeft(A(""), "")) return "Fail 9"
    if (!isNotEqualAnyLeft(A(""), null)) return "Fail 10"
    if (isNotEqualAnyLeft(A(""), A(""))) return "Fail 11"
    if (!isNotEqualAnyRight("", A(""))) return "Fail 12"
    if (!isNotEqualAnyRight(null, A(""))) return "Fail 13"
    if (isNotEqualAnyRight(A(""), A(""))) return "Fail 14"
    if (isNotEqualSameNullable(null, null)) return "Fail 15"
    if (isNotEqualSameNullable(A(""), A(""))) return "Fail 16"
    if (!isNotEqualSameNullable(null, A(""))) return "Fail 17"
    if (!isNotEqualSameNullable(A(""), null)) return "Fail 18"
    if (!isNotEqualSameNullable(A(""), A("a"))) return "Fail 19"
    if (isNotEqualAnyNullableLeft(null, null)) return "Fail 20"
    if (isNotEqualAnyNullableLeft(A(""), A(""))) return "Fail 21"
    if (!isNotEqualAnyNullableLeft(A(""), "")) return "Fail 22"
    if (!isNotEqualAnyNullableLeft(null, "")) return "Fail 23"
    if (!isNotEqualAnyNullableLeft(A(""), null)) return "Fail 24"
    if (!isNotEqualAnyNullableLeft(A(""), A("a"))) return "Fail 25"
    if (isNotEqualAnyNullableRight(null, null)) return "Fail 26"
    if (isNotEqualAnyNullableRight(A(""), A(""))) return "Fail 27"
    if (!isNotEqualAnyNullableRight("", A(""))) return "Fail 28"
    if (!isNotEqualAnyNullableRight("", null)) return "Fail 29"
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

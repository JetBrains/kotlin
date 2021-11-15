// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class A(val x: String)

fun isNullVacuousLeft(s: A) = s == null
fun isNullVacuousRight(s: A) = null == s
fun isNullLeft(s: A?) = s == null
fun isNullRight(s: A?) = null == s
fun isEqualSame(s: A, t: A) = s == t
fun isEqualAnyLeft(s: A, t: Any?) = s == t
fun isEqualAnyRight(s: Any?, t: A) = s == t
fun isEqualSameNullable(s: A?, t: A?) = s == t
fun isEqualAnyNullableLeft(s: A?, t: Any?) = s == t
fun isEqualAnyNullableRight(s: Any?, t: A?) = s == t
fun isEqualNullableUnboxedLeft(s: A, t: A?) = s == t
fun isEqualNullableUnboxedRight(s: A?, t: A) = s == t

fun box(): String {
    if (isNullVacuousLeft(A(""))) return "Fail 1"
    if (isNullVacuousRight(A(""))) return "Fail 2"
    if (isNullLeft(A(""))) return "Fail 3"
    if (isNullRight(A(""))) return "Fail 4"
    if (!isNullLeft(null)) return "Fail 5"
    if (!isNullRight(null)) return "Fail 6"
    if (!isEqualSame(A(""), A(""))) return "Fail 7"
    if (isEqualSame(A("a"), A("b"))) return "Fail 8"
    if (isEqualAnyLeft(A(""), "")) return "Fail 9"
    if (isEqualAnyLeft(A(""), null)) return "Fail 10"
    if (!isEqualAnyLeft(A(""), A(""))) return "Fail 11"
    if (isEqualAnyRight("", A(""))) return "Fail 12"
    if (isEqualAnyRight(null, A(""))) return "Fail 13"
    if (!isEqualAnyRight(A(""), A(""))) return "Fail 14"
    if (!isEqualSameNullable(null, null)) return "Fail 15"
    if (!isEqualSameNullable(A(""), A(""))) return "Fail 16"
    if (isEqualSameNullable(null, A(""))) return "Fail 17"
    if (isEqualSameNullable(A(""), null)) return "Fail 18"
    if (isEqualSameNullable(A(""), A("a"))) return "Fail 19"
    if (!isEqualAnyNullableLeft(null, null)) return "Fail 20"
    if (!isEqualAnyNullableLeft(A(""), A(""))) return "Fail 21"
    if (isEqualAnyNullableLeft(A(""), "")) return "Fail 22"
    if (isEqualAnyNullableLeft(null, "")) return "Fail 23"
    if (isEqualAnyNullableLeft(A(""), null)) return "Fail 24"
    if (isEqualAnyNullableLeft(A(""), A("a"))) return "Fail 25"
    if (!isEqualAnyNullableRight(null, null)) return "Fail 26"
    if (!isEqualAnyNullableRight(A(""), A(""))) return "Fail 27"
    if (isEqualAnyNullableRight("", A(""))) return "Fail 28"
    if (isEqualAnyNullableRight("", null)) return "Fail 29"
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

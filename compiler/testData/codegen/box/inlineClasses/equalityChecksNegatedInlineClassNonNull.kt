// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Inner(val w: String)
@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class A(val x: Inner)

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

fun box(): String {
    if (!isNotNullVacuousLeft(A(Inner("")))) return "Fail 1"
    if (!isNotNullVacuousRight(A(Inner("")))) return "Fail 2"
    if (!isNotNullLeft(A(Inner("")))) return "Fail 3"
    if (!isNotNullRight(A(Inner("")))) return "Fail 4"
    if (isNotNullLeft(null)) return "Fail 5"
    if (isNotNullRight(null)) return "Fail 6"
    if (isNotEqualSame(A(Inner("")), A(Inner("")))) return "Fail 7"
    if (!isNotEqualSame(A(Inner("")), A(Inner("a")))) return "Fail 8"
    if (!isNotEqualAnyLeft(A(Inner("")), Inner(""))) return "Fail 9"
    if (!isNotEqualAnyLeft(A(Inner("")), null)) return "Fail 10"
    if (isNotEqualAnyLeft(A(Inner("")), A(Inner("")))) return "Fail 11"
    if (!isNotEqualAnyRight(Inner(""), A(Inner("")))) return "Fail 12"
    if (!isNotEqualAnyRight(null, A(Inner("")))) return "Fail 13"
    if (isNotEqualAnyRight(A(Inner("")), A(Inner("")))) return "Fail 14"
    if (isNotEqualSameNullable(null, null)) return "Fail 15"
    if (isNotEqualSameNullable(A(Inner("")), A(Inner("")))) return "Fail 16"
    if (!isNotEqualSameNullable(null, A(Inner("")))) return "Fail 17"
    if (!isNotEqualSameNullable(A(Inner("")), null)) return "Fail 18"
    if (!isNotEqualSameNullable(A(Inner("")), A(Inner("a")))) return "Fail 19"
    if (isNotEqualAnyNullableLeft(null, null)) return "Fail 20"
    if (isNotEqualAnyNullableLeft(A(Inner("")), A(Inner("")))) return "Fail 21"
    if (!isNotEqualAnyNullableLeft(A(Inner("")), "")) return "Fail 22"
    if (!isNotEqualAnyNullableLeft(null, Inner(""))) return "Fail 23"
    if (!isNotEqualAnyNullableLeft(A(Inner("")), null)) return "Fail 24"
    if (!isNotEqualAnyNullableLeft(A(Inner("")), A(Inner("a")))) return "Fail 25"
    if (isNotEqualAnyNullableRight(null, null)) return "Fail 26"
    if (isNotEqualAnyNullableRight(A(Inner("a")), A(Inner("a")))) return "Fail 27"
    if (!isNotEqualAnyNullableRight(Inner(""), A(Inner("")))) return "Fail 28"
    if (!isNotEqualAnyNullableRight(Inner(""), null)) return "Fail 29"
    if (!isNotEqualAnyNullableRight(null, A(Inner("")))) return "Fail 30"
    if (!isNotEqualAnyNullableRight(A(Inner("a")), A(Inner("b")))) return "Fail 31"
    return "OK"
}

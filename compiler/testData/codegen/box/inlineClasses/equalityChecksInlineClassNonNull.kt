// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Inner(val w: String)
@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class A(val x: Inner)

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

fun box(): String {
    if (isNullVacuousLeft(A(Inner("")))) return "Fail 1"
    if (isNullVacuousRight(A(Inner("")))) return "Fail 2"
    if (isNullLeft(A(Inner("")))) return "Fail 3"
    if (isNullRight(A(Inner("")))) return "Fail 4"
    if (!isNullLeft(null)) return "Fail 5"
    if (!isNullRight(null)) return "Fail 6"
    if (!isEqualSame(A(Inner("")), A(Inner("")))) return "Fail 7"
    if (isEqualSame(A(Inner("")), A(Inner("a")))) return "Fail 8"
    if (isEqualAnyLeft(A(Inner("")), Inner(""))) return "Fail 9"
    if (isEqualAnyLeft(A(Inner("")), null)) return "Fail 10"
    if (!isEqualAnyLeft(A(Inner("")), A(Inner("")))) return "Fail 11"
    if (isEqualAnyRight(Inner(""), A(Inner("")))) return "Fail 12"
    if (isEqualAnyRight(null, A(Inner("")))) return "Fail 13"
    if (!isEqualAnyRight(A(Inner("")), A(Inner("")))) return "Fail 14"
    if (!isEqualSameNullable(null, null)) return "Fail 15"
    if (!isEqualSameNullable(A(Inner("")), A(Inner("")))) return "Fail 16"
    if (isEqualSameNullable(null, A(Inner("")))) return "Fail 17"
    if (isEqualSameNullable(A(Inner("")), null)) return "Fail 18"
    if (isEqualSameNullable(A(Inner("")), A(Inner("a")))) return "Fail 19"
    if (!isEqualAnyNullableLeft(null, null)) return "Fail 20"
    if (!isEqualAnyNullableLeft(A(Inner("")), A(Inner("")))) return "Fail 21"
    if (isEqualAnyNullableLeft(A(Inner("")), "")) return "Fail 22"
    if (isEqualAnyNullableLeft(null, Inner(""))) return "Fail 23"
    if (isEqualAnyNullableLeft(A(Inner("")), null)) return "Fail 24"
    if (isEqualAnyNullableLeft(A(Inner("")), A(Inner("a")))) return "Fail 25"
    if (!isEqualAnyNullableRight(null, null)) return "Fail 26"
    if (!isEqualAnyNullableRight(A(Inner("a")), A(Inner("a")))) return "Fail 27"
    if (isEqualAnyNullableRight(Inner(""), A(Inner("")))) return "Fail 28"
    if (isEqualAnyNullableRight(Inner(""), null)) return "Fail 29"
    if (isEqualAnyNullableRight(null, A(Inner("")))) return "Fail 30"
    if (isEqualAnyNullableRight(A(Inner("a")), A(Inner("b")))) return "Fail 31"
    return "OK"
}

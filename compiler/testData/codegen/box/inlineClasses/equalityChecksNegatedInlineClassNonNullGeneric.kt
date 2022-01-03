// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// IGNORE_BACKEND: JVM
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Inner<T: String>(val w: T)
OPTIONAL_JVM_INLINE_ANNOTATION
value class A<T: Inner<String>>(val x: T)

fun <T: Inner<String>> isNotNullVacuousLeft(s: A<T>) = s != null
fun <T: Inner<String>> isNotNullVacuousRight(s: A<T>) = null != s
fun <T: Inner<String>> isNotNullLeft(s: A<T>?) = s != null
fun <T: Inner<String>> isNotNullRight(s: A<T>?) = null != s
fun <T: Inner<String>> isNotEqualSame(s: A<T>, t: A<T>) = s != t
fun <T: Inner<String>> isNotEqualAnyLeft(s: A<T>, t: Any?) = s != t
fun <T: Inner<String>> isNotEqualAnyRight(s: Any?, t: A<T>) = s != t
fun <T: Inner<String>> isNotEqualSameNullable(s: A<T>?, t: A<T>?) = s != t
fun <T: Inner<String>> isNotEqualAnyNullableLeft(s: A<T>?, t: Any?) = s != t
fun <T: Inner<String>> isNotEqualAnyNullableRight(s: Any?, t: A<T>?) = s != t

fun box(): String {
    if (!isNotNullVacuousLeft(A(Inner("")))) return "Fail 1"
    if (!isNotNullVacuousRight(A(Inner("")))) return "Fail 2"
    if (!isNotNullLeft(A(Inner("")))) return "Fail 3"
    if (!isNotNullRight(A(Inner("")))) return "Fail 4"
    if (isNotNullLeft<Inner<String>>(null)) return "Fail 5"
    if (isNotNullRight<Inner<String>>(null)) return "Fail 6"
    if (isNotEqualSame(A(Inner("")), A(Inner("")))) return "Fail 7"
    if (!isNotEqualSame(A(Inner("")), A(Inner("a")))) return "Fail 8"
    if (!isNotEqualAnyLeft(A(Inner("")), Inner(""))) return "Fail 9"
    if (!isNotEqualAnyLeft(A(Inner("")), null)) return "Fail 10"
    if (isNotEqualAnyLeft(A(Inner("")), A(Inner("")))) return "Fail 11"
    if (!isNotEqualAnyRight(Inner(""), A(Inner("")))) return "Fail 12"
    if (!isNotEqualAnyRight(null, A(Inner("")))) return "Fail 13"
    if (isNotEqualAnyRight(A(Inner("")), A(Inner("")))) return "Fail 14"
    if (isNotEqualSameNullable<Inner<String>>(null, null)) return "Fail 15"
    if (isNotEqualSameNullable(A(Inner("")), A(Inner("")))) return "Fail 16"
    if (!isNotEqualSameNullable(null, A(Inner("")))) return "Fail 17"
    if (!isNotEqualSameNullable(A(Inner("")), null)) return "Fail 18"
    if (!isNotEqualSameNullable(A(Inner("")), A(Inner("a")))) return "Fail 19"
    if (isNotEqualAnyNullableLeft<Inner<String>>(null, null)) return "Fail 20"
    if (isNotEqualAnyNullableLeft(A(Inner("")), A(Inner("")))) return "Fail 21"
    if (!isNotEqualAnyNullableLeft(A(Inner("")), "")) return "Fail 22"
    if (!isNotEqualAnyNullableLeft<Inner<String>>(null, Inner(""))) return "Fail 23"
    if (!isNotEqualAnyNullableLeft(A(Inner("")), null)) return "Fail 24"
    if (!isNotEqualAnyNullableLeft(A(Inner("")), A(Inner("a")))) return "Fail 25"
    if (isNotEqualAnyNullableRight<Inner<String>>(null, null)) return "Fail 26"
    if (isNotEqualAnyNullableRight(A(Inner("a")), A(Inner("a")))) return "Fail 27"
    if (!isNotEqualAnyNullableRight(Inner(""), A(Inner("")))) return "Fail 28"
    if (!isNotEqualAnyNullableRight<Inner<String>>(Inner(""), null)) return "Fail 29"
    if (!isNotEqualAnyNullableRight(null, A(Inner("")))) return "Fail 30"
    if (!isNotEqualAnyNullableRight(A(Inner("a")), A(Inner("b")))) return "Fail 31"
    return "OK"
}

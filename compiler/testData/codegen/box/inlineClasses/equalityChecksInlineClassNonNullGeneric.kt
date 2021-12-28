// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Inner<T: String>(val w: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class A<T: Inner<String>>(val x: T)

fun <T: Inner<String>> isNullVacuousLeft(s: A<T>) = s == null
fun <T: Inner<String>> isNullVacuousRight(s: A<T>) = null == s
fun <T: Inner<String>> isNullLeft(s: A<T>?) = s == null
fun <T: Inner<String>> isNullRight(s: A<T>?) = null == s
fun <T: Inner<String>> isEqualSame(s: A<T>, t: A<T>) = s == t
fun <T: Inner<String>> isEqualAnyLeft(s: A<T>, t: Any?) = s == t
fun <T: Inner<String>> isEqualAnyRight(s: Any?, t: A<T>) = s == t
fun <T: Inner<String>> isEqualSameNullable(s: A<T>?, t: A<T>?) = s == t
fun <T: Inner<String>> isEqualAnyNullableLeft(s: A<T>?, t: Any?) = s == t
fun <T: Inner<String>> isEqualAnyNullableRight(s: Any?, t: A<T>?) = s == t

fun box(): String {
    if (isNullVacuousLeft(A(Inner("")))) return "Fail 1"
    if (isNullVacuousRight(A(Inner("")))) return "Fail 2"
    if (isNullLeft(A(Inner("")))) return "Fail 3"
    if (isNullRight(A(Inner("")))) return "Fail 4"
    if (!isNullLeft<Inner<String>>(null)) return "Fail 5"
    if (!isNullRight<Inner<String>>(null)) return "Fail 6"
    if (!isEqualSame(A(Inner("")), A(Inner("")))) return "Fail 7"
    if (isEqualSame(A(Inner("")), A(Inner("a")))) return "Fail 8"
    if (isEqualAnyLeft(A(Inner("")), Inner(""))) return "Fail 9"
    if (isEqualAnyLeft(A(Inner("")), null)) return "Fail 10"
    if (!isEqualAnyLeft(A(Inner("")), A(Inner("")))) return "Fail 11"
    if (isEqualAnyRight(Inner(""), A(Inner("")))) return "Fail 12"
    if (isEqualAnyRight(null, A(Inner("")))) return "Fail 13"
    if (!isEqualAnyRight(A(Inner("")), A(Inner("")))) return "Fail 14"
    if (!isEqualSameNullable<Inner<String>>(null, null)) return "Fail 15"
    if (!isEqualSameNullable(A(Inner("")), A(Inner("")))) return "Fail 16"
    if (isEqualSameNullable(null, A(Inner("")))) return "Fail 17"
    if (isEqualSameNullable(A(Inner("")), null)) return "Fail 18"
    if (isEqualSameNullable(A(Inner("")), A(Inner("a")))) return "Fail 19"
    if (!isEqualAnyNullableLeft<Inner<String>>(null, null)) return "Fail 20"
    if (!isEqualAnyNullableLeft(A(Inner("")), A(Inner("")))) return "Fail 21"
    if (isEqualAnyNullableLeft(A(Inner("")), "")) return "Fail 22"
    if (isEqualAnyNullableLeft<Inner<String>>(null, Inner(""))) return "Fail 23"
    if (isEqualAnyNullableLeft(A(Inner("")), null)) return "Fail 24"
    if (isEqualAnyNullableLeft(A(Inner("")), A(Inner("a")))) return "Fail 25"
    if (!isEqualAnyNullableRight<Inner<String>>(null, null)) return "Fail 26"
    if (!isEqualAnyNullableRight(A(Inner("a")), A(Inner("a")))) return "Fail 27"
    if (isEqualAnyNullableRight(Inner(""), A(Inner("")))) return "Fail 28"
    if (isEqualAnyNullableRight<Inner<String>>(Inner(""), null)) return "Fail 29"
    if (isEqualAnyNullableRight(null, A(Inner("")))) return "Fail 30"
    if (isEqualAnyNullableRight(A(Inner("a")), A(Inner("b")))) return "Fail 31"
    return "OK"
}

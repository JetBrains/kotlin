// LANGUAGE: -ReportErrorsForComparisonOperators

fun nullableNothingIdentity(a: Int, b: Nothing?) {
    <!SENSELESS_COMPARISON!>a === b<!>
}

fun samePrimitiveIdentity(a: Int, b: Int) {
    <!DEPRECATED_IDENTITY_EQUALS!>a === b<!>
}

fun identityWithImplicitBoxing(a: Int, b: Any?) {
    <!IMPLICIT_BOXING_IN_IDENTITY_EQUALS!>a === b<!>
}

enum class E1 { A, B }
enum class E2 { C, D }

fun nullableEnums(a: E1?, b: E2?) {
    <!INCOMPATIBLE_ENUM_COMPARISON!>a == b<!>
}

fun <T> enumAsTypeParameterBound(a: T, b: Int) where T : Any, T : <!ONLY_ONE_CLASS_BOUND_ALLOWED!>E1<!> {
    <!EQUALITY_NOT_APPLICABLE!>a == b<!>
}

fun <T, K> twoTypeParameters(a: T, b: K) where T : Number, K : <!FINAL_UPPER_BOUND!>String<!> {
    <!EQUALITY_NOT_APPLICABLE_WARNING!>a == b<!>
}

interface I1
interface I2

enum class E3 : I1 { A, B }

fun <A> compareTypeParameterWithEnum(a: A) where A: I1, A: I2 {
    <!INCOMPATIBLE_ENUM_COMPARISON!>a == E1.A<!>
    <!INCOMPATIBLE_ENUM_COMPARISON!>a == E3.A<!>
}

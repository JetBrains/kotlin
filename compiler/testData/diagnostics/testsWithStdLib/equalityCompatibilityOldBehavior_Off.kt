// LANGUAGE: +ReportErrorsForComparisonOperators

fun nullableNothingIdentity(a: Int, b: Nothing?) {
    a === <!DEBUG_INFO_CONSTANT!>b<!>
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
    a == b
}

fun <T> enumAsTypeParameterBound(a: T, b: Int) where T : Any, T : <!ONLY_ONE_CLASS_BOUND_ALLOWED!>E1<!> {
    <!EQUALITY_NOT_APPLICABLE!>a == b<!>
}

fun <T, K> twoTypeParameters(a: T, b: K) where T : Number, K : <!FINAL_UPPER_BOUND!>String<!> {
    a == b
}

interface I1
interface I2

enum class E3 : I1 { A, B }

fun <A> compareTypeParameterWithEnum(a: A) where A: I1, A: I2 {
    <!INCOMPATIBLE_ENUM_COMPARISON_ERROR!>a == E1.A<!>
    a == E3.A
}

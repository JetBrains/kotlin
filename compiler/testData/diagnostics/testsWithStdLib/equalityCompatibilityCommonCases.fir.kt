interface B

fun equalityNotApplicable(a: Int, b: B) {
    <!EQUALITY_NOT_APPLICABLE!>a == b<!>
}

fun equalityNotApplicableSmartCast(a: Any?, b: Any?) {
    if (a is Int && b is B) {
        <!EQUALITY_NOT_APPLICABLE_WARNING!>a == b<!>
    }
}

@JvmInline
value class C(val int: Int)
@JvmInline
value class D(val bool: Boolean)

fun forbiddenIdentityEquals(c: C, d: D) {
    <!FORBIDDEN_IDENTITY_EQUALS!>c === d<!>
}

fun forbiddenIdentityEqualsSmartCast(c: Any?, d: Any?) {
    if (c is C && d is D) {
        <!FORBIDDEN_IDENTITY_EQUALS_WARNING!>c === d<!>
    }
}

fun implicitBoxingInIdentityEquals(i: Int, a: Any?) {
    <!IMPLICIT_BOXING_IN_IDENTITY_EQUALS!>i === a<!>
}

fun implicitBoxingInIdentityEqualsSmartCast(i: Any?, a: Any?) {
    if (i is Int) {
        <!IMPLICIT_BOXING_IN_IDENTITY_EQUALS!>i === a<!>
    }
}

fun deprecatedIdentityEquals(a: Int, b: Int) {
    <!DEPRECATED_IDENTITY_EQUALS!>a === b<!>
}

fun deprecatedIdentityEqualsSmartCast(a: Any?, b: Any?) {
    if (a is Int && b is Int) {
        <!DEPRECATED_IDENTITY_EQUALS!>a === b<!>
    }
}

fun incompatibleTypes(a: Int) = when(a) {
    <!INCOMPATIBLE_TYPES!>C(10)<!> -> 1
    else -> 2
}

fun incompatibleTypesSmartCast(a: Any?) {
    if (a is Int) {
        when(a) {
            <!INCOMPATIBLE_TYPES!>C(10)<!> -> 1
            else -> 2
        }
    }
}

enum class E {
    A, B
}

fun incompatibleEnumComparison(c: B, e: E) {
    <!INCOMPATIBLE_ENUM_COMPARISON_ERROR!>c == e<!>
}

fun incompatibleEnumComparisonSmartCast(c: Any?, e: Any?) {
    if (c is B && e is E) {
        <!INCOMPATIBLE_ENUM_COMPARISON!>c == e<!>
    }
}

fun incompatibleIdentityRegardlessNullability(a: Int?, b: String?) {
    <!EQUALITY_NOT_APPLICABLE!>a == b<!>
    <!FORBIDDEN_IDENTITY_EQUALS!>a === b<!>
}

fun incompatibleIdentityRegardlessNullabilitySmartCast(a: Any?, b: Any?) {
    if (a is Int? && b is String?) {
        <!EQUALITY_NOT_APPLICABLE_WARNING!>a == b<!>
        <!FORBIDDEN_IDENTITY_EQUALS_WARNING!>a === b<!>
    }
}

fun incompatibleIdentityRegardlessNullabilityWithValueClasses(c: C?, d: D?) {
    <!EQUALITY_NOT_APPLICABLE!>c == d<!>
    <!FORBIDDEN_IDENTITY_EQUALS!>c === d<!>
}

fun incompatibleIdentityRegardlessNullabilityWithValueClassesSmartCast(c: Any?, d: Any?) {
    if (c is C? && d is D?) {
        <!EQUALITY_NOT_APPLICABLE_WARNING!>c == d<!>
        <!FORBIDDEN_IDENTITY_EQUALS_WARNING!>c === d<!>
    }
}

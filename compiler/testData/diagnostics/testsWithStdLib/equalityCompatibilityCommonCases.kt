interface B

fun equalityNotApplicable(a: Int, b: B) {
    <!EQUALITY_NOT_APPLICABLE!>a == b<!>
}

fun equalityNotApplicableSmartCast(a: Any?, b: Any?) {
    if (a is Int && b is B) {
        a == b
    }
}

@JvmInline
value class C(val int: Int)
@JvmInline
value class D(val bool: Boolean)

fun forbiddenIdentityEquals(c: C, d: D) {
    <!EQUALITY_NOT_APPLICABLE, FORBIDDEN_IDENTITY_EQUALS!>c === d<!>
}

fun forbiddenIdentityEqualsSmartCast(c: Any?, d: Any?) {
    if (c is C && d is D) {
        c === d
    }
}

fun implicitBoxingInIdentityEquals(i: Int, a: Any?) {
    <!IMPLICIT_BOXING_IN_IDENTITY_EQUALS!>i === a<!>
}

fun implicitBoxingInIdentityEqualsSmartCast(i: Any?, a: Any?) {
    if (i is Int) {
        i === a
    }
}

fun deprecatedIdentityEquals(a: Int, b: Int) {
    <!DEPRECATED_IDENTITY_EQUALS!>a === b<!>
}

fun deprecatedIdentityEqualsSmartCast(a: Any?, b: Any?) {
    if (a is Int && b is Int) {
        a === b
    }
}

fun incompatibleTypes(a: Int) = when(a) {
    <!INCOMPATIBLE_TYPES!>C(10)<!> -> 1
    else -> 2
}

fun incompatibleTypesSmartCast(a: Any?) {
    if (a is Int) {
        when(<!DEBUG_INFO_SMARTCAST!>a<!>) {
            C(10) -> 1
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
        c == e
    }
}

fun incompatibleIdentityRegardlessNullability(a: Int?, b: String?) {
    <!EQUALITY_NOT_APPLICABLE!>a == b<!>
    <!EQUALITY_NOT_APPLICABLE!>a === b<!>
}

fun incompatibleIdentityRegardlessNullabilitySmartCast(a: Any?, b: Any?) {
    if (a is Int? && b is String?) {
        a == b
        a === b
    }
}

fun incompatibleIdentityRegardlessNullabilityWithNull(a: Int?) {
    a == null
    a === null
}

fun incompatibleIdentityRegardlessNullabilityWithValueClasses(c: C?, d: D?) {
    <!EQUALITY_NOT_APPLICABLE!>c == d<!>
    <!EQUALITY_NOT_APPLICABLE, FORBIDDEN_IDENTITY_EQUALS!>c === d<!>
}

fun incompatibleIdentityRegardlessNullabilityWithValueClassesSmartCast(c: Any?, d: Any?) {
    if (c is C? && d is D?) {
        c == d
        c === d
    }
}

enum class F

fun <T: E, K: F> incompatibleEnumComparisonWithTypeParameters(e: T, f: K) {
    e == f
}

interface A
enum class G : B

fun <I : A> incompatibleEnumAndUnrelatedInterfaceThroughTypeParameter(x: G?, i: I?) {
    x == i
}

fun incompatibleEnumAndUnrelatedInterface(x: G?, i: A?) {
    x == i
}

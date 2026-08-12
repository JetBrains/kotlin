// RUN_PIPELINE_TILL: BACKEND

class A {
    override fun equals(@EqualityBound(A::class) other: Any?): Boolean = true
}

class B {
    override fun equals(@EqualityBound(B::class) other: Any?): Boolean = true
}

interface C {
    override fun equals(@EqualityBound(C::class) other: Any?): Boolean
}

open class D {
    override fun equals(@EqualityBound(D::class) other: Any?): Boolean = true
}

class E : D()

class F : D() {
    override fun equals(@EqualityBound(F::class) other: Any?): Boolean = true
}

interface G

fun direct(
    a: A, b: B, c: C, d: D, e: E, f: F, g: G,
): Boolean {
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>a == b<!>) return false
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>a == c<!>) return false
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>c == b<!>) return false
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>a == d<!>) return false
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>d == a<!>) return false
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>e == c<!>) return false
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>c == e<!>) return false
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>f == c<!>) return false
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>c == f<!>) return false
    if (<!EQUALITY_SUSPICIOUS_BY_EQUALITY_BOUNDS!>c == d<!>) return false
    if (c is D && c == d) return false
    if (<!EQUALITY_SUSPICIOUS_BY_EQUALITY_BOUNDS!>d == c<!>) return false
    if (d is C && c == d) return false
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>a == g<!>) return false
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>g == a<!>) return false
    if (g == d) return false
    if (d == g) return false
    if (g == c) return false
    if (c == g) return false
    return true
}

fun neq(
    a: A, b: B, c: C, d: D, e: E, f: F, g: G,
): Int {
    var i: Int = 0
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>a != b<!>) ++i
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>a != c<!>) ++i
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>c != b<!>) ++i
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>a != d<!>) ++i
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>d != a<!>) ++i
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>e != c<!>) ++i
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>c != e<!>) ++i
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>f != c<!>) ++i
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>c != f<!>) ++i
    if (<!EQUALITY_SUSPICIOUS_BY_EQUALITY_BOUNDS!>c != d<!>) ++i
    if (c !is D || c != d) ++i
    if (<!EQUALITY_SUSPICIOUS_BY_EQUALITY_BOUNDS!>d != c<!>) ++i
    if (c !is D || d != c) ++i
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>a != g<!>) ++i
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>g != a<!>) ++i
    if (g != d) ++i
    if (d != g) ++i
    if (g != c) ++i
    if (c != g) ++i
    return i
}

fun throughSmartcasts(
    a: Any, b: Any, c: Any, d: Any, e: Any, f: Any, g: Any,
): Boolean {
    if (a !is A || b !is B || c !is C || d !is D || e !is E || f !is F || g !is G) {
        return true
    }
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>a == b<!>) return false
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>a == c<!>) return false
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>c == b<!>) return false
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>a == d<!>) return false
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>d == a<!>) return false
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>e == c<!>) return false
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>c == e<!>) return false
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>f == c<!>) return false
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>c == f<!>) return false
    if (<!EQUALITY_SUSPICIOUS_BY_EQUALITY_BOUNDS!>c == d<!>) return false
    if (c is D && c == d) return false
    if (<!EQUALITY_SUSPICIOUS_BY_EQUALITY_BOUNDS!>d == c<!>) return false
    if (d is C && c == d) return false
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>a == g<!>) return false
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>g == a<!>) return false
    if (g == d) return false
    if (d == g) return false
    if (g == c) return false
    if (c == g) return false
    return true
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, equalityExpression, functionDeclaration, ifExpression,
interfaceDeclaration, nullableType, operator, override */

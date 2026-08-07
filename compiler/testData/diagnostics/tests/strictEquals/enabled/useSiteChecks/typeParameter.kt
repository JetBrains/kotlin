// RUN_PIPELINE_TILL: BACKEND

interface A {
    override fun equals(@EqualityBound(A::class) other: Any?): Boolean
}

open class B : A {
    override fun equals(other: Any?): Boolean = true
}

interface C {
    override fun equals(@EqualityBound(C::class) other: Any?): Boolean
}

open class D : C {
    override fun equals(@EqualityBound(D::class) other: Any?): Boolean = true
}

fun <TA : A, TB : B, TC: C, TD: D> foo(
    a: A, b: B, c: C, d: D,
    ta: TA, tb: TB, tc: TC, td: TD,
) {
    if (a == ta) return
    if (ta == a) return
    if (b == tb) return
    if (tb == b) return
    if (<!EQUALITY_SUSPICIOUS_BY_EQUALITY_BOUNDS!>c == ta<!>) return
    if (<!EQUALITY_SUSPICIOUS_BY_EQUALITY_BOUNDS!>tc == a<!>) return
    if (<!EQUALITY_SUSPICIOUS_BY_EQUALITY_BOUNDS!>tc == ta<!>) return
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>b == td<!>) return
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>tb == d<!>) return
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>tb == td<!>) return
    if (<!EQUALITY_SUSPICIOUS_BY_EQUALITY_BOUNDS!>tb == c<!>) return
    if (<!EQUALITY_SUSPICIOUS_BY_EQUALITY_BOUNDS!>tc == b<!>) return
    if (<!EQUALITY_SUSPICIOUS_BY_EQUALITY_BOUNDS!>tc == tb<!>) return
    if (tb == a) return
    if (ta == b) return
    if (tb == ta) return
    if (<!EQUALITY_SUSPICIOUS_BY_EQUALITY_BOUNDS!>td == a<!>) return
    if (<!EQUALITY_SUSPICIOUS_BY_EQUALITY_BOUNDS!>ta == d<!>) return
    if (<!EQUALITY_SUSPICIOUS_BY_EQUALITY_BOUNDS!>ta == td<!>) return
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, equalityExpression, functionDeclaration, ifExpression,
interfaceDeclaration, nullableType, operator, override, typeConstraint, typeParameter */

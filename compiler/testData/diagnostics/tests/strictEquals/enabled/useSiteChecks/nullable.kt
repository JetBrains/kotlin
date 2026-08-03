// RUN_PIPELINE_TILL: BACKEND

class A {
    override fun equals(@EqualityBound(A::class) other: Any?): Boolean = true
}

class B

interface C {
    override fun equals(@EqualityBound(C::class) other: Any?): Boolean
}

interface D : C {
    override fun equals(@EqualityBound(D::class) other: Any?): Boolean
}

fun foo(
    a: A, b: B, c: C, d: D,
    na: A?, nb: B?, nc: C?, nd: D?,
) {
    // nullability is actually ignored when reporting these warnings
    if (a == na) return
    if (na == a) return
    if (c == nc) return
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>a == nb<!>) return
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>nb == a<!>) return
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>na == nb<!>) return
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>a == nc<!>) return
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>na == c<!>) return
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>na == nc<!>) return
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>b == nc<!>) return
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>nb == c<!>) return
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>nb == nc<!>) return
    if (c == nd) return
    if (nc == d) return
    if (nc == nd) return
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, equalityExpression, functionDeclaration, ifExpression,
interfaceDeclaration, nullableType, operator, override */

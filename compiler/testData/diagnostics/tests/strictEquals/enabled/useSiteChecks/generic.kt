// RUN_PIPELINE_TILL: BACKEND

interface A<T> {
    override fun equals(@EqualityBound(A::class) other: Any?): Boolean
}

class B<T> : A<T> {
    override fun equals(@EqualityBound(B::class) other: Any?): Boolean = true
}

class C<T> : A<T> {
    override fun equals(other: Any?): Boolean = true
}


fun test(
    a: A<String>, a2: A<Number>,
    b: B<String>, b2: B<Number>,
    c: C<String>, c2: C<Number>,
) {
    if (a == b) return
    if (b == a) return
    if (a2 == b) return
    if (b2 == a) return
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>c == b<!>) return
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>b == c<!>) return
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>c2 == b<!>) return
    if (<!EQUALITY_NOT_APPLICABLE_BY_EQUALITY_BOUNDS!>b == c2<!>) return
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, equalityExpression, functionDeclaration, ifExpression,
interfaceDeclaration, nullableType, operator, override, typeParameter */

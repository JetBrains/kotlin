// RUN_PIPELINE_TILL: FRONTEND

interface I1<T> {
    fun i1(): T
    override fun equals(@EqualityBound(I1::class) other: Any?): Boolean
}

interface I2 : I1<String> {
    override fun equals(other: Any?): Boolean
}

fun useSite_1(a: I2, b: I2?, c: Any?) {
    if (a.equals(c)) {
        c.i1().<!UNRESOLVED_REFERENCE!>length<!>
    }
    if (b?.equals(c) == true) {
        c.i1().<!UNRESOLVED_REFERENCE!>length<!>
    }
}

/* GENERATED_FIR_TAGS: capturedType, classReference, equalityExpression, functionDeclaration, ifExpression,
interfaceDeclaration, nullableType, operator, override, safeCall, smartcast, starProjection, typeParameter */

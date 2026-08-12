// RUN_PIPELINE_TILL: FRONTEND

interface I1 {
    override fun equals(@EqualityBound(I1::class) other: Any?): Boolean
}

interface I2

class Bad : I1, I2 {
    <!EQUALITY_BOUND_MISMATCH_ON_INHERITANCE("'fun equals(other: Any?): Boolean' defined in 'Bad'; 'fun equals(other: Any?): Boolean' defined in 'I1'")!>override fun equals(@EqualityBound(I2::class) other: Any?): Boolean = true<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, functionDeclaration, interfaceDeclaration, nullableType,
operator, override */

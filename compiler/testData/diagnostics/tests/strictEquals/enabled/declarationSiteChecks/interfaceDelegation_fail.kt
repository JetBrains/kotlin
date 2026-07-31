// RUN_PIPELINE_TILL: FRONTEND

// FILE: f6.kt

package p6

interface I1 {
    override fun equals(@EqualityBound(I1::class) other: Any?): Boolean
}

interface I2 {
    override fun equals(@EqualityBound(I2::class) other: Any?): Boolean
}

class Impl : I1 {
    override fun equals(@EqualityBound(I1::class) other: Any?): Boolean = true
}

<!EQUALITY_BOUND_MISMATCH_BY_DELEGATION("'fun equals(other: Any?): Boolean' defined in 'p6.Bad'; 'fun equals(other: Any?): Boolean' defined in 'p6.I2'")!>class Bad(impl: Impl) : I1 by impl, I2<!>

/* GENERATED_FIR_TAGS: classDeclaration, classReference, functionDeclaration, inheritanceDelegation,
interfaceDeclaration, nullableType, operator, override, primaryConstructor */

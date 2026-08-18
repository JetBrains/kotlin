// RUN_PIPELINE_TILL: FRONTEND

interface I {
    override fun equals(@EqualityBound(I::class) other: Any?): Boolean
}

open class A : I {
    override fun equals(@EqualityBound(I::class) other: Any?): Boolean = true
}

interface J : I {
    override fun equals(@EqualityBound(J::class) other: Any?): Boolean
}

<!EQUALITY_BOUND_MISMATCH_ON_INHERITANCE("'fun equals(other: Any?): Boolean' defined in 'A'; 'fun equals(other: Any?): Boolean' defined in 'J'")!>class B : A(), J<!>

/* GENERATED_FIR_TAGS: classDeclaration, classReference, functionDeclaration, interfaceDeclaration, nullableType,
operator, override */

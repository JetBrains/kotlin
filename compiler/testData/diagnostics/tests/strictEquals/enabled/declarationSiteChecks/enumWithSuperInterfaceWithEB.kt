// RUN_PIPELINE_TILL: FRONTEND

interface IF {
    override fun equals(@EqualityBound(IF::class) other: Any?): Boolean
}

<!EQUALITY_BOUND_MISMATCH_ON_INHERITANCE!>enum class E : IF {
    X;
}<!>

/* GENERATED_FIR_TAGS: classReference, enumDeclaration, enumEntry, functionDeclaration, interfaceDeclaration,
nullableType, operator, override */

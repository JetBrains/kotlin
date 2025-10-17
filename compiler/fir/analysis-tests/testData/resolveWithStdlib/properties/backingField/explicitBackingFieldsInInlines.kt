// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -NOTHING_TO_INLINE

class Test {
    val numbers: List<Int>
        field = mutableListOf()

    inline fun testPublic(): MutableList<*> = <!RETURN_TYPE_MISMATCH!>numbers<!>

    internal inline fun testInternal(): MutableList<*> = numbers

    protected inline fun testProtected(): MutableList<*> = <!RETURN_TYPE_MISMATCH!>numbers<!>

    private inline fun testPrivate(): MutableList<*> = numbers
}

/* GENERATED_FIR_TAGS: classDeclaration, explicitBackingField, functionDeclaration, inline, propertyDeclaration,
smartcast, starProjection */

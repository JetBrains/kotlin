// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// ISSUE: KT-80445

<!EXPLICIT_FIELD_VISIBILITY_MUST_BE_LESS_PERMISSIVE!>private<!> val b: List<String>
    field = mutableListOf()

private class C {
    val a: List<String>
        field = mutableListOf()
}

private class A {
    class B {
        val a: List<String>
            field = mutableListOf()

        fun mustWork() = a.add("test")
    }

    fun mustNotWork() = B().a.<!UNRESOLVED_REFERENCE!>add<!>("test")
}

/* GENERATED_FIR_TAGS: classDeclaration, explicitBackingField, functionDeclaration, nestedClass, propertyDeclaration,
smartcast, stringLiteral */

// RUN_PIPELINE_TILL: FRONTEND

enum class E {
    ENTRY
}

class Foo {
    override fun equals(@EqualityBound(<!ANNOTATION_ARGUMENT_MUST_BE_KCLASS_LITERAL!>E.ENTRY::class<!>) other: Any?): Boolean = true
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, enumDeclaration, enumEntry, functionDeclaration, nullableType,
operator, override */

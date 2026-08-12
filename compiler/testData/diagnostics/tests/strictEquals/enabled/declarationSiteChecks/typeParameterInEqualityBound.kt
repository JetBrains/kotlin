// RUN_PIPELINE_TILL: FRONTEND

class A<T> {
    override fun equals(@EqualityBound(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, TYPE_PARAMETER_AS_REIFIED!>T::class<!>) other: Any?): Boolean = true
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, functionDeclaration, nullableType, operator, override,
typeParameter */

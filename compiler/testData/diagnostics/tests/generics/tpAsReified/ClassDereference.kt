// RUN_PIPELINE_TILL: FRONTEND

fun <T: Any> dereferenceClass(): Any =
        <!TYPE_PARAMETER_AS_REIFIED!>T::class<!>

/* GENERATED_FIR_TAGS: classReference, functionDeclaration, typeConstraint, typeParameter */

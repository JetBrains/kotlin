// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
annotation class Ann(
        val a: Int,
        <!VAR_ANNOTATION_PARAMETER!>var<!> b: Int,
        <!MISSING_VAL_ON_ANNOTATION_PARAMETER!>c: String<!>
        )

/* GENERATED_FIR_TAGS: annotationDeclaration, primaryConstructor, propertyDeclaration */

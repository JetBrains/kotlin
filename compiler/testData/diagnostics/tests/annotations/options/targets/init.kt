// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
annotation class base

@base class My {
    <!WRONG_ANNOTATION_TARGET!>@base<!> init {
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, init */

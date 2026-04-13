// RUN_PIPELINE_TILL: FRONTEND
annotation class base

@base class My {
    <!WRONG_ANNOTATION_TARGET!>@base<!> init {
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, init */

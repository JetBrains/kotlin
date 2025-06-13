// RUN_PIPELINE_TILL: FRONTEND
annotation class AnnKlass

class Child : <!FINAL_SUPERTYPE!>AnnKlass<!>

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration */

// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: FRONTEND
annotation class AnnKlass

class <!INHERITING_AN_ANNOTATION_CLASS_WARNING!>Child<!> : <!FINAL_SUPERTYPE!>AnnKlass<!>

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration */

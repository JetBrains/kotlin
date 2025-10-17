// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: FRONTEND
annotation class AnnKlass

class Child : <!FINAL_SUPERTYPE, INHERITING_AN_ANNOTATION_CLASS_ERROR!>AnnKlass<!>

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration */

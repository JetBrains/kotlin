// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ProhibitExtendingAnnotationClasses
annotation class AnnKlass

class <!EXTENDING_AN_ANNOTATION_CLASS_ERROR!>Child<!> : <!FINAL_SUPERTYPE!>AnnKlass<!>

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration */

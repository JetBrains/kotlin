// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -FINAL_SUPERTYPE
// This error needs to be suppressed to cause light class generation
// LANGUAGE: +ProhibitExtendingAnnotationClasses

class <!EXTENDING_AN_ANNOTATION_CLASS_ERROR!>Foo<!> : Target()

/* GENERATED_FIR_TAGS: classDeclaration */

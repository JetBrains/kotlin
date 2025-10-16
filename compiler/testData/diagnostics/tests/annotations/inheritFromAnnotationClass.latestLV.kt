// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -FINAL_SUPERTYPE
// This error needs to be suppressed to cause light class generation

class <!INHERITING_AN_ANNOTATION_CLASS_ERROR!>Foo<!> : Target()

/* GENERATED_FIR_TAGS: classDeclaration */

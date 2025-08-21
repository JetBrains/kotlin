// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// FILE: script.kts

annotation class Ann

@Ann<!SYNTAX!><!>

/* GENERATED_FIR_TAGS: annotationDeclaration, init, localProperty, propertyDeclaration */

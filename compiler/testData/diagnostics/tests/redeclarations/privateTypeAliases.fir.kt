// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KTIJ-25422

// FILE: A1.kt
private typealias <!CLASSIFIER_REDECLARATION!>A<!> = Int

// FILE: A2.kt
private typealias <!CLASSIFIER_REDECLARATION!>A<!> = String

/* GENERATED_FIR_TAGS: classDeclaration */

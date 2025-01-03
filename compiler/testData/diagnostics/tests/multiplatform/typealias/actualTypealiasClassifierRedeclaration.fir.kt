// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt
expect class A {}

// MODULE: jvm()()(common)
// FILE: main.kt
actual typealias <!CLASSIFIER_REDECLARATION!>A<!> = B

class B {}

class <!ACTUAL_MISSING, CLASSIFIER_REDECLARATION!>A<!> {}

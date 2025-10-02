// ISSUE: KT-20677
// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_FIR_DIAGNOSTICS
// MODULE: m1-common

expect annotation class Ann

@<!UNRESOLVED_REFERENCE!>Ann<!>
fun commonFoo() {}

// MODULE: m1-jvm()()(m1-common)

@<!UNRESOLVED_REFERENCE!>Ann<!>
fun platformFoo() {}

/* GENERATED_FIR_TAGS: annotationDeclaration, expect, functionDeclaration */

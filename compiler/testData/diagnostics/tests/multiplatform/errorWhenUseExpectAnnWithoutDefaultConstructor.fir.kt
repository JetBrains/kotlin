// ISSUE: KT-20677
// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_FIR_DIAGNOSTICS
// MODULE: m1-common

<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect<!> annotation class Ann

<!NO_CONSTRUCTOR!>@Ann<!>
fun commonFoo() {}

// MODULE: m1-jvm()()(m1-common)

<!NO_CONSTRUCTOR!>@Ann<!>
fun platformFoo() {}

/* GENERATED_FIR_TAGS: annotationDeclaration, expect, functionDeclaration */

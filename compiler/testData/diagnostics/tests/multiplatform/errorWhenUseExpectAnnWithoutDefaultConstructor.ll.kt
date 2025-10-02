// ISSUE: KT-20677
// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_FIR_DIAGNOSTICS
// MODULE: m1-common

expect annotation class Ann

<!NO_IMPLICIT_DEFAULT_CONSTRUCTOR_ON_EXPECT_ANNOTATION_CLASS!>@Ann<!>
fun commonFoo() {}

// MODULE: m1-jvm()()(m1-common)

<!NO_IMPLICIT_DEFAULT_CONSTRUCTOR_ON_EXPECT_ANNOTATION_CLASS!>@Ann<!>
fun platformFoo() {}

/* GENERATED_FIR_TAGS: annotationDeclaration, expect, functionDeclaration */

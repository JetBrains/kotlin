// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt
expect fun <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>main<!>()

// FILE: common2.kt
actual fun <!ACTUAL_WITHOUT_EXPECT, EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>main<!>() {}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
<!UNSUPPORTED_FEATURE!>expect fun <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE, EXPECT_REFINEMENT_ANNOTATION_MISSING!>main<!>()<!>

// FILE: jvm2.kt
actual fun <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>main<!>() {}

/* GENERATED_FIR_TAGS: actual, expect, functionDeclaration */

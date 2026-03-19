// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-45332

// KT-45332: MPP: "expect function has no actual declaration for JVM" with Android target

<!NOT_A_MULTIPLATFORM_COMPILATION!>expect<!> fun <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>platform<!>(): String

<!NOT_A_MULTIPLATFORM_COMPILATION!>actual<!> fun <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>platform<!>(): String = "Android"

/* GENERATED_FIR_TAGS: actual, expect, functionDeclaration, stringLiteral */

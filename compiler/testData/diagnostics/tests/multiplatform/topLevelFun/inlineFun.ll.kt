// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt

inline expect fun inlineFun()
expect fun nonInlineFun()

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual fun <!EXPECT_ACTUAL_INCOMPATIBLE_FUNCTION_MODIFIERS_NOT_SUBSET!>inlineFun<!>() { }
actual fun nonInlineFun() { }

/* GENERATED_FIR_TAGS: actual, expect, functionDeclaration, inline */

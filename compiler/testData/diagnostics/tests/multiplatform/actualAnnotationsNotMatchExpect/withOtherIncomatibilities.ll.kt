// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt
annotation class Ann

@Ann
expect inline fun hasWeakIncompatibility()

@Ann
expect fun hasStrongIncompatibility(arg: Int)

expect fun hasStrongIncompatibility(arg: Double)

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT, EXPECT_ACTUAL_INCOMPATIBLE_FUNCTION_MODIFIERS_NOT_SUBSET!>hasWeakIncompatibility<!>() {}

actual fun <!ACTUAL_WITHOUT_EXPECT!>hasStrongIncompatibility<!>(arg: Any?) {}

actual fun hasStrongIncompatibility(arg: Double) {}

/* GENERATED_FIR_TAGS: actual, annotationDeclaration, expect, functionDeclaration, inline, nullableType */

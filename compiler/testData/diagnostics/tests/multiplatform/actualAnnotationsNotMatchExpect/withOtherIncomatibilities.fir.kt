// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt
annotation class Ann

@Ann
<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect<!> inline fun hasWeakIncompatibility()

@Ann
<!EXPECT_ACTUAL_MISMATCH{JVM}, EXPECT_ACTUAL_MISMATCH{JVM}!>expect<!> fun hasStrongIncompatibility(arg: Int)

expect fun hasStrongIncompatibility(arg: Double)

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual<!> fun <!ACTUAL_WITHOUT_EXPECT!>hasWeakIncompatibility<!>() {}

actual fun <!ACTUAL_WITHOUT_EXPECT!>hasStrongIncompatibility<!>(arg: Any?) {}

actual fun hasStrongIncompatibility(arg: Double) {}

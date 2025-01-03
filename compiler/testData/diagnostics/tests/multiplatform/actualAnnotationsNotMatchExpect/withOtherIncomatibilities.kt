// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt
annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Ann<!>

<!CONFLICTING_OVERLOADS!>@Ann
expect inline fun hasWeakIncompatibility()<!>

<!CONFLICTING_OVERLOADS!>@Ann
expect fun hasStrongIncompatibility<!NO_ACTUAL_FOR_EXPECT{JVM}!>(arg: Int)<!><!>

<!CONFLICTING_OVERLOADS!>expect fun hasStrongIncompatibility(arg: Double)<!>

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
<!ACTUAL_WITHOUT_EXPECT!>actual<!> fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>hasWeakIncompatibility<!>() {}

actual fun hasStrongIncompatibility<!ACTUAL_WITHOUT_EXPECT!>(arg: Any?)<!> {}

actual fun hasStrongIncompatibility(arg: Double) {}

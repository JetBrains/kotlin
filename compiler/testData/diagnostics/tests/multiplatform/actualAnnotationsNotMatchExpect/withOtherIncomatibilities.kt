// MODULE: m1-common
// FILE: common.kt
annotation class Ann

@Ann
expect inline fun hasWeakIncompatibility()

@Ann
expect fun hasStrongIncompatibility<!NO_ACTUAL_FOR_EXPECT{JVM}!>(arg: Int)<!>

expect fun hasStrongIncompatibility(arg: Double)

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
<!ACTUAL_WITHOUT_EXPECT!>actual<!> fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>hasWeakIncompatibility<!>() {}

actual fun hasStrongIncompatibility<!ACTUAL_WITHOUT_EXPECT!>(arg: Any?)<!> {}

actual fun hasStrongIncompatibility(arg: Double) {}

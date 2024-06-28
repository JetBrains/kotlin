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
actual fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT, ACTUAL_WITHOUT_EXPECT!>hasWeakIncompatibility<!>() {}

actual fun <!ACTUAL_WITHOUT_EXPECT!>hasStrongIncompatibility<!>(arg: Any?) {}

actual fun hasStrongIncompatibility(arg: Double) {}

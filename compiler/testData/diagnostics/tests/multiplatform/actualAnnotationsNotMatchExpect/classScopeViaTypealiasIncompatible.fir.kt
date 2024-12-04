// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt
annotation class Ann

<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect<!> class WeakIncompatibility {
    @Ann
    fun <!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>foo<!>(p: String)
}

<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect<!> class StrongIncompatibility {
    @Ann
    fun <!EXPECT_ACTUAL_MISMATCH{JVM}!>foo<!>(p: Int)
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
class WeakIncompatibilityImpl {
    fun foo(differentName: String) {}
}

<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual<!> typealias <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>WeakIncompatibility<!> = WeakIncompatibilityImpl

class StrongIncompatibilityImpl {
    fun foo(p: String) {} // Different param type
}

actual typealias <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>StrongIncompatibility<!> = StrongIncompatibilityImpl

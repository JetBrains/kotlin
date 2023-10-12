// MODULE: m1-common
// FILE: common.kt
annotation class Ann

<!INCOMPATIBLE_EXPECT_ACTUAL{JVM}!>expect class WeakIncompatibility {
    <!INCOMPATIBLE_EXPECT_ACTUAL{JVM}!>@Ann
    fun foo(p: String)<!>
}<!>

<!INCOMPATIBLE_EXPECT_ACTUAL{JVM}!>expect class StrongIncompatibility {
    <!EXPECT_ACTUAL_MISMATCH{JVM}!>@Ann
    fun foo(p: Int)<!>
}<!>

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
class WeakIncompatibilityImpl {
    fun foo(differentName: String) {}
}

<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual typealias <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT, NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>WeakIncompatibility<!> = WeakIncompatibilityImpl<!>

class StrongIncompatibilityImpl {
    fun foo(p: String) {} // Different param type
}

actual typealias <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT, NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>StrongIncompatibility<!> = StrongIncompatibilityImpl

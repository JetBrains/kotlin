// MODULE: m1-common
// FILE: common.kt
<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect class DefaultArgsInNestedClass {
    <!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>annotation class Nested<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>(val p: String = "")<!><!>
}<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
class DefaultArgsInNestedClassImpl {
    annotation class Nested(val p: String = "")
}

// Incompatible because of bug KT-31636
actual typealias <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>DefaultArgsInNestedClass<!> = DefaultArgsInNestedClassImpl

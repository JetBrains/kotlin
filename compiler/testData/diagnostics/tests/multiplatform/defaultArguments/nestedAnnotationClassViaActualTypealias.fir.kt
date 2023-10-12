// MODULE: m1-common
// FILE: common.kt
<!INCOMPATIBLE_EXPECT_ACTUAL{JVM}!>expect class DefaultArgsInNestedClass {
    <!INCOMPATIBLE_EXPECT_ACTUAL{JVM}!>annotation class Nested<!INCOMPATIBLE_EXPECT_ACTUAL{JVM}!>(val p: String = "")<!><!>
}<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
class DefaultArgsInNestedClassImpl {
    annotation class Nested(val p: String = "")
}

// Incompatible because of bug KT-31636
actual typealias DefaultArgsInNestedClass = DefaultArgsInNestedClassImpl

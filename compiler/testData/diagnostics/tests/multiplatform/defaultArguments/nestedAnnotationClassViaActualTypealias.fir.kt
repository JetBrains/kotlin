// TODO: test fails, fix in subsequent commits
// MODULE: m1-common
// FILE: common.kt
<!INCOMPATIBLE_MATCHING{JVM}!>expect class DefaultArgsInNestedClass {
    <!INCOMPATIBLE_MATCHING{JVM}!>annotation class Nested<!INCOMPATIBLE_MATCHING{JVM}!>(val p: String = "")<!><!>
}<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
class DefaultArgsInNestedClassImpl {
    annotation class Nested(val p: String = "")
}

// Incompatible because of bug KT-31636
actual typealias DefaultArgsInNestedClass = DefaultArgsInNestedClassImpl

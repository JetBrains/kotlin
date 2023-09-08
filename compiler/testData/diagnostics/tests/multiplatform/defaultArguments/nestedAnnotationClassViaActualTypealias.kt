// TODO: test fails, fix in subsequent commits
// MODULE: m1-common
// FILE: common.kt
expect class DefaultArgsInNestedClass {
    annotation class Nested(val p: String = "")
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
class DefaultArgsInNestedClassImpl {
    annotation class Nested(val p: String = "")
}

// Incompatible because of bug KT-31636
actual typealias <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>DefaultArgsInNestedClass<!> = DefaultArgsInNestedClassImpl

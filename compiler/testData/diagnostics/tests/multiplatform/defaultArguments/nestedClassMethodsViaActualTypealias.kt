// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt
expect class DefaultArgsInNestedClass {
    class Nested {
        fun foo(p: String = "")
    }
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
class DefaultArgsInNestedClassImpl {
    class Nested {
        fun foo(p: String) {}
    }
}

<!DEFAULT_ARGUMENTS_IN_EXPECT_WITH_ACTUAL_TYPEALIAS!>actual typealias DefaultArgsInNestedClass = DefaultArgsInNestedClassImpl<!>

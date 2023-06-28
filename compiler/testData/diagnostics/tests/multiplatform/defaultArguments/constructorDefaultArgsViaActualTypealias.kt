// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt
expect class DefaultArgsInConstructor(p1: String = "common", p2: String = "common", p3: String)

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
class DefaultArgsInConstructorImpl(p1: String = "common", p2: String = "common", p3: String)

<!DEFAULT_ARGUMENTS_IN_EXPECT_WITH_ACTUAL_TYPEALIAS!>actual typealias <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>DefaultArgsInConstructor<!> = DefaultArgsInConstructorImpl<!>

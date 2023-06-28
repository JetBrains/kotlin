// !DIAGNOSTICS: -ACTUAL_WITHOUT_EXPECT
// MODULE: m1-common
// FILE: common.kt
expect class A {
    fun foo(p1: String = "common", p2: String = "common", p3: String)
}

expect class B {
    fun foo(s: String)
}

interface I {
    fun methodWithDefaultArg(s: String = "common")
}

expect class WithDefaultArgFromSuper : I {
    override fun methodWithDefaultArg(s: String)
}

expect open class WithIncompatibility {
    fun foo(p: String = "common")
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

class AImpl {
    fun foo(p1: String = "impl", p2: String = "impl", p3: String) {}
}

<!DEFAULT_ARGUMENTS_IN_EXPECT_WITH_ACTUAL_TYPEALIAS!>actual typealias <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>A<!> = AImpl<!>

class BImpl {
    fun foo(s: String = "impl") {}
}

actual typealias <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>B<!> = BImpl

class WithDefaultArgFromSuperImpl : I {
    override fun methodWithDefaultArg(s: String) {}
}

actual typealias WithDefaultArgFromSuper = WithDefaultArgFromSuperImpl

class WithIncompatibilityImpl {
    fun foo(p: String) {}
}

<!DEFAULT_ARGUMENTS_IN_EXPECT_WITH_ACTUAL_TYPEALIAS!>actual typealias WithIncompatibility = WithIncompatibilityImpl<!>

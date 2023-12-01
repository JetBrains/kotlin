// MODULE: m1-common
// FILE: common.kt
<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect class Foo {
    <!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>fun foo(param: Int = 1)<!>
    <!NO_ACTUAL_FOR_EXPECT{JVM}!>fun missingOnActual()<!>
}<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
open class Base {
    fun foo(param: Int) {}
}

actual class <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>Foo<!> : <!DEFAULT_ARGUMENTS_IN_EXPECT_ACTUALIZED_BY_FAKE_OVERRIDE!>Base()<!>

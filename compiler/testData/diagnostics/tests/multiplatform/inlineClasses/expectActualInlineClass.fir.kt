// !LANGUAGE: +InlineClasses, -JvmInlineValueClasses
// MODULE: m1-common
// FILE: common.kt

expect inline class Foo1(val x: Int) {
    fun bar(): String
}

expect inline class Foo2(val x: Int)

expect <!ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS!>inline<!> class Foo3

expect class NonInlineExpect

expect inline class NonInlineActual(val x: Int)

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual inline class Foo1(val x: Int) {
    actual fun bar(): String = "Hello"
}
actual inline class <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>Foo2<!>(val x: String)
actual <!ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_VALUE_CLASS!>inline<!> class Foo3

<!ACTUAL_WITHOUT_EXPECT!>actual inline class NonInlineExpect(val x: Int)<!>

<!ACTUAL_WITHOUT_EXPECT!>actual class NonInlineActual actual constructor(actual val x: Int)<!>

// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-58845
// IGNORE_DIAGNOSTIC_API
// IGNORE_REVERSED_RESOLVE

// MODULE: common
// FILE: common.kt
expect fun interface F1 {
    fun foo()
}

expect fun interface F2 {
    fun foo()
}

expect fun interface F3 {
    fun foo(a: Int)
}

<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect fun interface F4 {
    <!EXPECT_ACTUAL_MISMATCH{JVM}!>fun foo()<!>
}<!>

// MODULE: jvm()()(common)
// FILE: main.kt
actual fun interface F1 {
    <!FUN_INTERFACE_CANNOT_HAVE_ABSTRACT_PROPERTIES!>val<!> a: Int
    actual fun foo()
}

actual fun interface F2 {
    actual fun foo()
    <!FUN_INTERFACE_CANNOT_HAVE_ABSTRACT_PROPERTIES!>val<!> a: Int
}

actual fun interface F3 {
    actual fun foo(<!ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS, FUN_INTERFACE_ABSTRACT_METHOD_WITH_DEFAULT_VALUE!>a: Int = 0<!>)
}

actual fun interface <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>F4<!> {
    actual fun <!FUN_INTERFACE_ABSTRACT_METHOD_WITH_TYPE_PARAMETERS!><T><!> <!ACTUAL_WITHOUT_EXPECT!>foo<!>()
}

// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-58845
// IGNORE_DIAGNOSTIC_API
// IGNORE_REVERSED_RESOLVE

// MODULE: common
// FILE: common.kt
expect fun interface <!NO_ACTUAL_FOR_EXPECT!>F1<!> {
    fun foo()
}

expect fun interface <!NO_ACTUAL_FOR_EXPECT!>F2<!> {
    fun foo()
}

expect fun interface <!NO_ACTUAL_FOR_EXPECT!>F3<!> {
    fun foo(a: Int)
}

expect fun interface <!NO_ACTUAL_FOR_EXPECT!>F4<!> {
    fun foo()
}

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
    actual fun <!ACTUAL_WITHOUT_EXPECT, FUN_INTERFACE_ABSTRACT_METHOD_WITH_TYPE_PARAMETERS!><T><!> foo()
}

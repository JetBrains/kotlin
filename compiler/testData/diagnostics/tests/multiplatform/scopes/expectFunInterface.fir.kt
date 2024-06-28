// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-58845

// MODULE: common
// FILE: common.kt
expect fun interface F1 {
    fun foo()
}

expect <!FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS!>fun<!> interface F2

expect fun interface F3 {
    fun foo()
}

<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect fun interface F4 {
    <!NO_ACTUAL_FOR_EXPECT{JVM}!>fun foo()<!>
}<!>

expect <!FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS!>fun<!> interface F5

// MODULE: jvm()()(common)
// FILE: main.kt
interface I {
    fun bar()
}

actual <!FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS!>fun<!> interface F1 {
    actual fun foo()
    fun bar()
}

actual fun interface F2 {
    fun bar()
}

actual <!FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS!>fun<!> interface F3 : I {
    actual fun foo()
}

actual <!FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS!>fun<!> interface <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>F4<!> {}

actual <!FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS!>fun<!> interface F5 {}

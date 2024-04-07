// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-58845

// MODULE: common
// FILE: common.kt
expect fun interface <!NO_ACTUAL_FOR_EXPECT!>F1<!> {
    fun foo()
}

expect <!FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS, FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS{JVM}!>fun<!> interface <!NO_ACTUAL_FOR_EXPECT!>F2<!>

expect fun interface <!NO_ACTUAL_FOR_EXPECT!>F3<!> {
    fun foo()
}

expect fun interface <!NO_ACTUAL_FOR_EXPECT!>F4<!> {
    fun foo()
}

expect <!FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS, FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS{JVM}!>fun<!> interface <!NO_ACTUAL_FOR_EXPECT!>F5<!>

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

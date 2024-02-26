// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-58845
// Reason: MPP diagnostics are reported differentely in the compiler and AA

// MODULE: common
// FILE: common.kt
expect interface <!NO_ACTUAL_FOR_EXPECT!>I1<!>

expect interface <!NO_ACTUAL_FOR_EXPECT!>I2<!>

expect interface <!NO_ACTUAL_FOR_EXPECT!>I3<!>

<!FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS{JVM}!>fun<!> interface F1 : I1 {
    fun foo()
}

<!FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS!>fun<!> interface F2 : I2 {}

<!FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS, FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS{JVM}!>fun<!> interface F3 : I3 {}

// MODULE: jvm()()(common)
// FILE: main.kt
actual interface I1 {
    fun bar()
}

actual interface I2 {
    fun bar()
}

actual interface I3 {}

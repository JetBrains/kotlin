// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-58845

// MODULE: common
// FILE: common.kt
expect interface <!NO_ACTUAL_FOR_EXPECT!>I1<!>

expect interface <!NO_ACTUAL_FOR_EXPECT!>I2<!>

expect interface <!NO_ACTUAL_FOR_EXPECT!>I3<!>

<!FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS!>fun<!> interface F1 : I1, I2 {}

<!FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS, FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS{JVM}!>fun<!> interface F2 : I1, I3 {}

// MODULE: jvm()()(common)
// FILE: main.kt
actual interface I1 {
    fun foo()
}

actual interface I2 {
    fun foo()
}

actual interface I3 {
    fun bar()
}

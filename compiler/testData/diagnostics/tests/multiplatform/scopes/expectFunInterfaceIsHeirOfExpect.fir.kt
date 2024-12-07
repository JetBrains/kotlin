// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-58845

// MODULE: common
// FILE: common.kt
expect interface I1

<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect<!> <!FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS!>fun<!> interface F1 : I1 {}

expect interface I2

<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect<!> fun interface F2 : I2 {
    fun foo()
}

// MODULE: jvm()()(common)
// FILE: main.kt
actual interface I1 {
    fun bar()
}

actual <!FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS!>fun<!> interface <!ACTUAL_WITHOUT_EXPECT!>F1<!> : I1 {
    fun baz()
}

actual interface I2 : I1 {}

actual <!FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS!>fun<!> interface <!ACTUAL_WITHOUT_EXPECT!>F2<!> : I2 {
    actual fun foo()
}

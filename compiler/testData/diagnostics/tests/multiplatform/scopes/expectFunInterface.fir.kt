// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-58845

// MODULE: common
// FILE: common.kt
<!EXPECT_ACTUAL_IR_INCOMPATIBILITY{JVM}!>expect<!> fun interface F1 {
    fun foo()
}

expect <!FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS!>fun<!> interface F2

<!EXPECT_ACTUAL_IR_INCOMPATIBILITY{JVM}!>expect<!> fun interface F3 {
    fun foo()
}

<!EXPECT_ACTUAL_IR_INCOMPATIBILITY{JVM}!>expect<!> fun interface F4 {
    fun <!NO_ACTUAL_FOR_EXPECT{JVM}!>foo<!>()
}

<!EXPECT_ACTUAL_IR_INCOMPATIBILITY{JVM}!>expect<!> <!FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS!>fun<!> interface F5

// MODULE: jvm()()(common)
// FILE: main.kt
interface I {
    fun bar()
}

actual <!FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS!>fun<!> interface <!EXPECT_ACTUAL_INCOMPATIBLE_FUN_INTERFACE_MODIFIER!>F1<!> {
    actual fun foo()
    fun bar()
}

actual fun interface F2 {
    fun bar()
}

actual <!FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS!>fun<!> interface <!EXPECT_ACTUAL_INCOMPATIBLE_FUN_INTERFACE_MODIFIER!>F3<!> : I {
    actual fun foo()
}

actual <!FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS!>fun<!> interface <!EXPECT_ACTUAL_INCOMPATIBLE_FUN_INTERFACE_MODIFIER, NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>F4<!> {}

actual <!FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS!>fun<!> interface <!EXPECT_ACTUAL_INCOMPATIBLE_FUN_INTERFACE_MODIFIER!>F5<!> {}

/* GENERATED_FIR_TAGS: actual, expect, funInterface, functionDeclaration, interfaceDeclaration */

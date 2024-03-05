// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-58845
// MUTE_LL_FIR
//  Reason: MPP diagnostics are reported differentely in the compiler and AA

// MODULE: common
// FILE: common.kt
expect interface <!NO_ACTUAL_FOR_EXPECT!>I<!>

<!FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS{JVM}!>fun<!> interface F : I {
    fun foo()
}

// MODULE: jvm()()(common)
// FILE: main.kt

actual interface I {
    fun bar()
}

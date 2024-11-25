// RUN_PIPELINE_TILL: FIR2IR
// ISSUE: KT-68830
// MUTE_LL_FIR: LL tests don't run IR actualizer to report NO_ACTUAL_FOR_EXPECT
// MODULE: m1-common
// FILE: common.kt

open expect class <!NO_ACTUAL_FOR_EXPECT{JVM}!>A<!>() {
    open fun foo(): String
}

expect class <!NO_ACTUAL_FOR_EXPECT{JVM}!>B<!>() : A

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

fun test1() = B().foo()

fun test2() = with(B()) {
    foo()
}

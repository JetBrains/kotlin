// RUN_PIPELINE_TILL: FIR2IR
// ISSUE: KT-68830
// MUTE_LL_FIR: LL tests don't run IR actualizer to report NO_ACTUAL_FOR_EXPECT
// MODULE: m1-common
// FILE: common.kt

open expect class A1() {
    open fun foo(): String
}

expect class <!NO_ACTUAL_FOR_EXPECT{JVM}!>B1<!>() : A1

fun test1() = B1().foo()

open class A2() {
    open fun foo(): String = "OK"
}

expect class <!NO_ACTUAL_FOR_EXPECT{JVM}!>B2<!>() : A2

fun test2() = B2().foo()

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

open actual class A1 {
    open actual fun foo(): String = "OK"
}

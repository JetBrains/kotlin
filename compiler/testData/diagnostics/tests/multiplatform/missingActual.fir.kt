// ISSUE: KT-68830
// MUTE_LL_FIR: LL tests don't run IR actualizer to report NO_ACTUAL_FOR_EXPECT
// MODULE: m1-common
// FILE: common.kt

<!NO_ACTUAL_FOR_EXPECT{JVM}!>open expect class A() {
    open fun foo(): String
}<!>

<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect class B() : A<!>

fun test() = B().foo()

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

fun box(): String {
    test()
    return "OK"
}

// RUN_PIPELINE_TILL: FIR2IR
// ISSUE: KT-68830
// MODULE: m1-common
// FILE: common.kt

open <!NO_ACTUAL_FOR_EXPECT{JVM}!>expect<!> class A() {
    open fun foo(): String
}

<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect<!> class B() : A

fun test() = B().foo()

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

fun box(): String {
    test()
    return "OK"
}

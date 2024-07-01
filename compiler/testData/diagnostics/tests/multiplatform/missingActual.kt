// ISSUE: KT-68830
// MODULE: m1-common
// FILE: common.kt

open expect class <!NO_ACTUAL_FOR_EXPECT{JVM}!>A<!>() {
    open fun foo(): String
}

expect class <!NO_ACTUAL_FOR_EXPECT{JVM}!>B<!>() : A

fun test() = B().foo()

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

fun box(): String {
    test()
    return "OK"
}

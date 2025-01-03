// RUN_PIPELINE_TILL: FIR2IR
// ISSUE: KT-68830
// MUTE_LL_FIR: LL tests don't run IR actualizer to report NO_ACTUAL_FOR_EXPECT
// MODULE: m1-common
// FILE: common.kt

open expect class <!NO_ACTUAL_FOR_EXPECT{JVM}, PACKAGE_OR_CLASSIFIER_REDECLARATION!>A<!>() {
    open fun foo(): String
}

expect class <!NO_ACTUAL_FOR_EXPECT{JVM}, PACKAGE_OR_CLASSIFIER_REDECLARATION!>B<!>() : A

<!CONFLICTING_OVERLOADS!>fun test()<!> = B().foo()

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

fun box(): String {
    test()
    return "OK"
}

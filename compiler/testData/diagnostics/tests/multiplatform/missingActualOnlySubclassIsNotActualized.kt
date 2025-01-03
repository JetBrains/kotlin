// RUN_PIPELINE_TILL: FIR2IR
// ISSUE: KT-68830
// MUTE_LL_FIR: LL tests don't run IR actualizer to report NO_ACTUAL_FOR_EXPECT
// MODULE: m1-common
// FILE: common.kt

open expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>A1<!>() {
    open fun foo(): String
}

expect class <!NO_ACTUAL_FOR_EXPECT{JVM}, PACKAGE_OR_CLASSIFIER_REDECLARATION!>B1<!>() : A1

<!CONFLICTING_OVERLOADS!>fun test1()<!> = B1().foo()

open class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>A2<!>() {
    open fun foo(): String = "OK"
}

expect class <!NO_ACTUAL_FOR_EXPECT{JVM}, PACKAGE_OR_CLASSIFIER_REDECLARATION!>B2<!>() : A2

<!CONFLICTING_OVERLOADS!>fun test2()<!> = B2().foo()

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

open actual class A1 {
    open actual fun foo(): String = "OK"
}

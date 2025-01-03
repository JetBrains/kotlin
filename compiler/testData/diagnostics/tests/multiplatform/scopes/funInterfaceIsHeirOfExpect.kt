// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-58845
// Reason: MPP diagnostics are reported differentely in the compiler and AA

// MODULE: common
// FILE: common.kt
expect interface <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>I1<!>

expect interface <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>I2<!>

expect interface <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>I3<!>

<!FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS{JVM}!>fun<!> interface <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>F1<!> : I1 {
    fun foo()
}

<!FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS!>fun<!> interface <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>F2<!> : I2 {}

<!FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS, FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS{JVM}!>fun<!> interface <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>F3<!> : I3 {}

// MODULE: jvm()()(common)
// FILE: main.kt
actual interface I1 {
    fun bar()
}

actual interface I2 {
    fun bar()
}

actual interface I3 {}

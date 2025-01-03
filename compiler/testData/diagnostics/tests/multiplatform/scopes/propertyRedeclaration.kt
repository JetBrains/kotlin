// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>A<!> {
    val x: Int
}

expect abstract class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>B<!>

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>C<!> : B

expect abstract class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>D<!>() {
    val <!AMBIGUOUS_ACTUALS{JVM}!>x<!>: Int
}

class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>E<!> : D()

// MODULE: jvm()()(common)
// FILE: main.kt
interface I {
    val x: Int
}

actual class A : I {
    actual val <!VIRTUAL_MEMBER_HIDDEN!>x<!> = 0
}

actual abstract class B() {
    val x = 0
}

actual class C : B(), I {}

actual abstract class D {
    actual val <!REDECLARATION!>x<!> = 0
    val <!ACTUAL_MISSING, REDECLARATION!>x<!> = 0
}

// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>A<!> {
    fun foo()
}

expect abstract class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>B<!>

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>C<!> : B

expect abstract class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>D<!>() {
    fun <!AMBIGUOUS_ACTUALS{JVM}!>foo<!>()
}

class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>E<!> : D()

// MODULE: jvm()()(common)
// FILE: main.kt
interface I {
    fun foo()
}

actual class A : I {
    actual fun <!VIRTUAL_MEMBER_HIDDEN!>foo<!>() {}
}

actual abstract class B() {
    fun foo() {}
}

actual class C : B(), I {}

actual abstract class D {
    <!CONFLICTING_OVERLOADS!>actual fun foo()<!> {}
    <!CONFLICTING_OVERLOADS!>fun <!ACTUAL_MISSING!>foo<!>()<!> {}
}

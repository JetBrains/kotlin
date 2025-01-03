// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FRONTEND
// MODULE: common
// FILE: common.kt
expect interface <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>I<!>
expect interface <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>J<!>
<!MANY_IMPL_MEMBER_NOT_IMPLEMENTED{JVM}!>class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>X<!><!>(a: I, b : J): I by a, J by b

// MODULE: platform()()(common)
// FILE: platform.kt
actual interface I {
    fun foo()
}
actual interface J {
    fun foo()
}

// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt
@Target(AnnotationTarget.TYPE)
annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Ann<!>

<!CONFLICTING_OVERLOADS!>expect fun foo(): @Ann Int<!>

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual fun foo() = 1

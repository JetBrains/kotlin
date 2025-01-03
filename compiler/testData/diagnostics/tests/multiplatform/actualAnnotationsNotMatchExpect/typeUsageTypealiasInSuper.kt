// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt
@Target(AnnotationTarget.TYPE)
annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Ann<!>

interface <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>I<!>

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo<!>: @Ann I

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
typealias ITypealias = I

actual class Foo : ITypealias

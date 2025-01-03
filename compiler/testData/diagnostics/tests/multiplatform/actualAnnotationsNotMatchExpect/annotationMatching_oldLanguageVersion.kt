// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -MultiplatformRestrictions
// MODULE: m1-common
// FILE: common.kt
annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Ann<!>

@Ann
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>AnnotationOnExpectOnly<!>

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>AnnotationInside<!> {
    @Ann
    fun onlyOnExpect()
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

actual class AnnotationOnExpectOnly

actual class AnnotationInside {
    actual fun onlyOnExpect() {}
}

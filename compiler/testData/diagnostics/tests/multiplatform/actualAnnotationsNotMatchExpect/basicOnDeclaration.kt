// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt
annotation class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Ann<!>

@Ann
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>AnnotationMatching<!>

@Ann
expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>AnnotationOnExpectOnly<!>

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>AnnotationOnActualOnly<!>

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>AnnotationInside<!> {
    @Ann
    fun matches()

    @Ann
    fun onlyOnExpect()

    fun onlyOnActual()
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
@Ann
actual class AnnotationMatching

actual class <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>AnnotationOnExpectOnly<!>

@Ann
actual class AnnotationOnActualOnly

actual class AnnotationInside {
    @Ann
    actual fun matches() {}

    actual fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>onlyOnExpect<!>() {}

    @Ann
    actual fun onlyOnActual() {}
}

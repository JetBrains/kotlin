// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
expect annotation class ExpectIsSubsetOfActual

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
expect annotation class ExpectIsSubsetOfActualDifferentOrder

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
expect annotation class MoreTargetsOnExpect

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
expect annotation class RepeatedTargetsInExpect

@Target(allowedTargets = [])
expect annotation class EmptyTargetsActual

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
@Target(AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.CLASS)
actual annotation class ExpectIsSubsetOfActual

@Target(AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.CLASS)
actual annotation class ExpectIsSubsetOfActualDifferentOrder

@Target(AnnotationTarget.FUNCTION)
actual annotation class <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>MoreTargetsOnExpect<!>

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS, AnnotationTarget.TYPEALIAS)
actual annotation class RepeatedTargetsInExpect

@Target(AnnotationTarget.FUNCTION)
actual annotation class EmptyTargetsActual

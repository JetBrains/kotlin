// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.PROPERTY_GETTER,
    // removed target TYPEALIAS
)
expect annotation class MyDeprecatedNotMatch

@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.TYPEALIAS
)
expect annotation class MyDeprecatedMatch

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual typealias MyDeprecatedNotMatch = kotlin.Deprecated

actual typealias MyDeprecatedMatch = kotlin.Deprecated

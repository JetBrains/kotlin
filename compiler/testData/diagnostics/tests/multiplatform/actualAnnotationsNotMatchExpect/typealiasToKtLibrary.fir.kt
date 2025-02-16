// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// DIAGNOSTICS: -ACTUAL_TYPEALIAS_TO_SPECIAL_ANNOTATION, -ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_MEMBERS_AS_NON_FINAL_EXPECT_CLASSIFIER_WARNING, -ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_SUPERTYPES_AS_NON_FINAL_EXPECT_CLASSIFIER_WARNING
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
    AnnotationTarget.TYPEALIAS,
    AnnotationTarget.TYPE, // added target
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

annotation class Ann(val s: String)

expect abstract class MyAbstractIterator<T> {
    @Ann("something" + "complex")
    fun hasNext(): Boolean
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual<!> typealias MyDeprecatedNotMatch = kotlin.Deprecated

actual typealias MyDeprecatedMatch = kotlin.Deprecated

<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual<!> typealias MyAbstractIterator<T> = AbstractIterator<T>

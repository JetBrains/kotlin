// ISSUE: KT-73255
// LANGUAGE: -PropertyParamAnnotationDefaultTargetMode

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD)
annotation class SomeField

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY)
annotation class SomeProperty

class My(@SomeField @SomeProperty val x: String)

annotation class Your(@SomeField @SomeProperty val value: String)

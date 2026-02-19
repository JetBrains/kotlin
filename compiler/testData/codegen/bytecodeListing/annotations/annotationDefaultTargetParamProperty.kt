// IGNORE_BACKEND_K1: JVM_IR
// ISSUE: KT-73255 (not supported in K1)
// LANGUAGE: +PropertyParamAnnotationDefaultTargetMode

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD)
annotation class SomeField

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY)
annotation class SomeProperty

class My(@SomeField @SomeProperty val x: String)

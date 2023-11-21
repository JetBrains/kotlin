// ISSUE: KT-73255
// LANGUAGE: -PropertyParamAnnotationDefaultTargetMode
// JVM_ABI_K1_K2_DIFF: K2 stores annotations in metadata (KT-57919).

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD)
annotation class SomeField

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY)
annotation class SomeProperty

class My(@SomeField @SomeProperty val x: String)

annotation class Your(@SomeField @SomeProperty val value: String)

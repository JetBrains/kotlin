// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-70233

@Target(AnnotationTarget.FIELD)
annotation class FieldAnnotation

@Target(AnnotationTarget.PROPERTY)
annotation class PropertyAnnotation

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class ParameterAnnotation

annotation class A(
    @FieldAnnotation
    @PropertyAnnotation
    @ParameterAnnotation
    val x: Int
)

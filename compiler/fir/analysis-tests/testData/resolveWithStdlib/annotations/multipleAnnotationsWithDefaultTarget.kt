// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +PropertyParamAnnotationDefaultTargetMode

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
annotation class ParamPropertyField

annotation class WithoutExplicitTarget

class MultipleTargets (@[ParamPropertyField WithoutExplicitTarget] val value: String)
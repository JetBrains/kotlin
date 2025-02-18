// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +PropertyParamAnnotationDefaultTargetMode, +AnnotationAllUseSiteTarget

@Repeatable
annotation class WithoutExplicitTarget

@Repeatable
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
annotation class ParamPropertyField

class Test(@all:WithoutExplicitTarget  @WithoutExplicitTarget val a: String,
           @param:WithoutExplicitTarget @all:WithoutExplicitTarget val b: String,
           @param:WithoutExplicitTarget @WithoutExplicitTarget val c: String,
           @all:ParamPropertyField @ParamPropertyField val d: String,
           @all:ParamPropertyField @param:ParamPropertyField val e: String,
           @ParamPropertyField @param:ParamPropertyField val f: String)
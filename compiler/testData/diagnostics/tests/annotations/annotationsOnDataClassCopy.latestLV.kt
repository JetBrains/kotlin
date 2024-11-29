// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FIR_DUMP
// LATEST_LV_DIFFERENCE

annotation class NoTarget

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Param

@Target(AnnotationTarget.PROPERTY)
annotation class Prop

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
annotation class Both

data class Foo(
    <!ANNOTATION_WILL_BE_APPLIED_ALSO_TO_PROPERTY_OR_FIELD("property")!>@NoTarget<!> @Param @Prop <!ANNOTATION_WILL_BE_APPLIED_ALSO_TO_PROPERTY_OR_FIELD("property")!>@Both<!> val p1: Int,
    @param:NoTarget @param:Both val p2: String,
    @property:NoTarget @property:Both val p3: Boolean,
)
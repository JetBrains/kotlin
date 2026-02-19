// MyClass
// LANGUAGE: +AnnotationAllUseSiteTarget

annotation class MyClass(
    @all:Param
    @all:Property
    @all:PropertyAndField
    @all:ParameterPropertyAndField
    @all:Get
    @all:Everything
    val pr<caret>op: Int,
)

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Param

@Target(AnnotationTarget.PROPERTY)
annotation class Property

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
annotation class PropertyAndField

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
annotation class ParameterPropertyAndField

@Target(AnnotationTarget.PROPERTY_GETTER)
annotation class Get

@Target(
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
)
annotation class Everything

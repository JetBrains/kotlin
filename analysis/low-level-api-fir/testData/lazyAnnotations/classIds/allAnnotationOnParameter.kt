// QUERY: classIds
// MEMBER_NAME_FILTER: prop
// RESOLVE_PROPERTY_PART: BACKING_FIELD

class My<caret>Class(
    @all:Param
    @all:Property
    @all:Field
    @all:PropertyAndField
    @all:ParameterPropertyAndField
    @all:Get
    @all:Everything
    @all:Deprecated("Obsolete")
    var prop: Int,
)

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Param

@Target(AnnotationTarget.PROPERTY)
annotation class Property

@Target(AnnotationTarget.FIELD)
annotation class Field

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

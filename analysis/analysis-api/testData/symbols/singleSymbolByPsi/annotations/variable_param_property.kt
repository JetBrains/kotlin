// LANGUAGE: +PropertyParamAnnotationDefaultTargetMode
// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1

@Property
@Field
@PropertyAndField
@ParameterPropertyAndField
@Everything
var pr<caret>op: Int = 1

@Target(AnnotationTarget.PROPERTY)
annotation class Property

@Target(AnnotationTarget.FIELD)
annotation class Field

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
annotation class PropertyAndField

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
annotation class ParameterPropertyAndField

@Target(
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
)
annotation class Everything

// LANGUAGE: -PropertyParamAnnotationDefaultTargetMode
// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1

@Property
@PropertyAndField
@ParameterPropertyAndField
@Everything
var pr<caret>op: Int
    get() = 1
    set(value) {}

@Target(AnnotationTarget.PROPERTY)
annotation class Property

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

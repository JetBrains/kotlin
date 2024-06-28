package myPack

@Target(
    AnnotationTarget.PROPERTY,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.FIELD,
)
annotation class Anno(val number: Int)

@Anno(propertyProperty)
const val propertyProperty = 42

@Anno(getterProperty)
const val getterProperty = 42

@Anno(setterProperty)
const val setterProperty = 42

@Anno(setterParameterProperty)
const val setterParameterProperty = 42

@Anno(fieldProperty)
const val fieldProperty = 42

@Anno(propertyProperty)
@field:Anno(fieldProperty)
var variable<caret>ToResolve = "${42}"
    @Anno(getterProperty)
    get() = field + "str"
    @Anno(setterProperty)
    set(@Anno(setterParameterProperty) value) = Unit

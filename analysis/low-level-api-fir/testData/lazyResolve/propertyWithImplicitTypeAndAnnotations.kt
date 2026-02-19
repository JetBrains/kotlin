package myPack

@Target(
    AnnotationTarget.PROPERTY,
    AnnotationTarget.TYPE_PARAMETER,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.TYPE,
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

@Anno(receiverProperty)
const val receiverProperty = 42

@Anno(receiverTypeProperty)
const val receiverTypeProperty = 42

@Anno(typeParameterProperty)
const val typeParameterProperty = 42

@Anno(propertyProperty)
var <@Anno(typeParameterProperty) T> @receiver:Anno(receiverProperty) @Anno(receiverTypeProperty) T.vari<caret>ableToResolve
    @Anno(getterProperty)
    get() = "str"
    @Anno(setterProperty)
    set(@Anno(setterParameterProperty) value) = Unit

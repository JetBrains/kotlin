package myPack

@Target(
    AnnotationTarget.PROPERTY,
    AnnotationTarget.TYPE_PARAMETER,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.TYPE,
)
annotation class Anno(val number: Int)

@Anno(functionProperty)
const val functionProperty = 42

@Anno(parameterProperty)
const val parameterProperty = 42

@Anno(defaultValueProperty)
const val defaultValueProperty = 42

@Anno(receiverProperty)
const val receiverProperty = 42

@Anno(receiverTypeProperty)
const val receiverTypeProperty = 42

@Anno(typeParameterProperty)
const val typeParameterProperty = 42

@Anno(valueParameterTypeProperty)
const val valueParameterTypeProperty = 42

@Anno(functionProperty)
fun <@Anno(typeParameterProperty) T> @receiver:Anno(receiverProperty) @Anno(receiverTypeProperty) Int.fun<caret>ction(@Anno(parameterProperty) param: @Anno(valueParameterTypeProperty) Int = defaultValueProperty) = "str"
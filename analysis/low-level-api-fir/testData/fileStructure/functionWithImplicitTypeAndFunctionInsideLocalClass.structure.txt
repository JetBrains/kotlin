package myPack

@Target(
    AnnotationTarget.PROPERTY,
    AnnotationTarget.TYPE_PARAMETER,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.TYPE,
    AnnotationTarget.EXPRESSION,
)
@Retention(AnnotationRetention.SOURCE)
annotation class Anno(val number: Int)/* DeclarationStructureElement *//* ClassDeclarationStructureElement */

@Anno(functionProperty)
const val functionProperty = 42/* DeclarationStructureElement */

@Anno(parameterProperty)
const val parameterProperty = 42/* DeclarationStructureElement */

@Anno(defaultValueProperty)
const val defaultValueProperty = 42/* DeclarationStructureElement */

@Anno(receiverProperty)
const val receiverProperty = 42/* DeclarationStructureElement */

@Anno(receiverTypeProperty)
const val receiverTypeProperty = 42/* DeclarationStructureElement */

@Anno(typeParameterProperty)
const val typeParameterProperty = 42/* DeclarationStructureElement */

@Anno(valueParameterTypeProperty)
const val valueParameterTypeProperty = 42/* DeclarationStructureElement */

@Anno(expressionProperty)
const val expressionProperty = 42/* DeclarationStructureElement */

fun topLevelFun() {/* DeclarationStructureElement */
    class LocalClass {
        fun first() = 42.function()

        @Anno(functionProperty)
        fun <@Anno(typeParameterProperty) T> @receiver:Anno(receiverProperty) @Anno(receiverTypeProperty) T.function(
            @Anno(parameterProperty) param: @Anno(
                valueParameterTypeProperty
            ) String = "${defaultValueProperty}",
        ) = @Anno(expressionProperty) "str"
    }
}

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
annotation class Anno(val number: Int)/* DeclarationStructureElement *//* ClassDeclarationStructureElement */

@Anno(propertyProperty)
const val propertyProperty = 42/* DeclarationStructureElement */

@Anno(getterProperty)
const val getterProperty = 42/* DeclarationStructureElement */

@Anno(setterProperty)
const val setterProperty = 42/* DeclarationStructureElement */

@Anno(setterParameterProperty)
const val setterParameterProperty = 42/* DeclarationStructureElement */

@Anno(receiverProperty)
const val receiverProperty = 42/* DeclarationStructureElement */

@Anno(receiverTypeProperty)
const val receiverTypeProperty = 42/* DeclarationStructureElement */

@Anno(typeParameterProperty)
const val typeParameterProperty = 42/* DeclarationStructureElement */

fun topLevelFun() {/* DeclarationStructureElement */

    class LocalClass {
        fun first() = 42.variableToResolve

        @Anno(propertyProperty)
        var <@Anno(typeParameterProperty) T> @receiver:Anno(receiverProperty) @Anno(receiverTypeProperty) T.variableToResolve
            @Anno(getterProperty)
            get() = "str"
            @Anno(setterProperty)
            set(@Anno(setterParameterProperty) value) = Unit
    }
}

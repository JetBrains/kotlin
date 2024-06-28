package myPack/* RootScriptStructureElement */

@Target(
    AnnotationTarget.PROPERTY,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.FIELD,
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

@Anno(fieldProperty)
const val fieldProperty = 42/* DeclarationStructureElement */

fun topLevelFun() {/* DeclarationStructureElement */
class LocalClass {
    fun first() = variableToResolve

    @Anno(propertyProperty)
    @field:Anno(fieldProperty)
    var variableToResolve = "${42}"
        @Anno(getterProperty)
        get() = field + "str"
        @Anno(setterProperty)
        set(@Anno(setterParameterProperty) value) = Unit
}
}

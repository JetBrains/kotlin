package myPack/* RootScriptStructureElement */

@Target(AnnotationTarget.LOCAL_VARIABLE, AnnotationTarget.EXPRESSION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class Anno(val number: Int)/* DeclarationStructureElement *//* ClassDeclarationStructureElement */

@Anno(localAnnotationProperty)
const val localAnnotationProperty = 42/* DeclarationStructureElement */

@Anno(expressionAnnotationProperty)
const val expressionAnnotationProperty = 42/* DeclarationStructureElement */

@Anno(setterAnnotationProperty)
const val setterAnnotationProperty = 42/* DeclarationStructureElement */

@Anno(receiverAnnotationProperty)
const val receiverAnnotationProperty = 42/* DeclarationStructureElement */

fun topLevelFunction() = run {
    @Anno(localAnnotationProperty)
    var @receiver:Anno(receiverAnnotationProperty) Int.variableToResolve = @Anno(expressionAnnotationProperty) "str"
}/* DeclarationStructureElement */

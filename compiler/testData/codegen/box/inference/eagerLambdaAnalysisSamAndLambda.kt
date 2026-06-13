// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +UnitConversionsOnArbitraryExpressions, +InferThrowableTypeParameterToUpperBound

fun unitLambdaAndTypeSam(block: () -> Unit) = "O"
fun unitLambdaAndTypeSam(block: Sam) = "K"

fun interface Sam {
    fun run(): Type
}

object Type

fun box(): String {
    return unitLambdaAndTypeSam { Unit } + unitLambdaAndTypeSam { Type }
}

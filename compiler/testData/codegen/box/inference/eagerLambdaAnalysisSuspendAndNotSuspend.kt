// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +UnitConversionsOnArbitraryExpressions, +InferThrowableTypeParameterToUpperBound

fun stringSuspendAndUnitNotSuspend(block: suspend () -> String) = "O"
fun stringSuspendAndUnitNotSuspend(block: () -> Unit) = "K"

fun box(): String {
    return stringSuspendAndUnitNotSuspend { "" } + stringSuspendAndUnitNotSuspend { Unit }
}

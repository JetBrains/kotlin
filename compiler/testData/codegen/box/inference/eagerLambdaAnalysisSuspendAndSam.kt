// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +UnitConversionsOnArbitraryExpressions, +InferThrowableTypeParameterToUpperBound

fun suspendUnitLambdaAndStringSam(block: suspend () -> Unit) = "O"
fun suspendUnitLambdaAndStringSam(block: StringSam) = "K"

fun interface StringSam {
    fun run(): String
}

fun box(): String {
    return suspendUnitLambdaAndStringSam { Unit } + suspendUnitLambdaAndStringSam { "" }
}

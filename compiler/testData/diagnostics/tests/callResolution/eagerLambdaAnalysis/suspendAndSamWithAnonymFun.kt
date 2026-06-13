// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +UnitConversionsOnArbitraryExpressions, +InferThrowableTypeParameterToUpperBound
// ISSUES: KT-86325

fun a(block: suspend () -> Unit) = 1
fun a(block: StringSam) = "(2)"

fun interface StringSam {
    fun run(): String
}

fun test() {
    a { fun() {} }
}

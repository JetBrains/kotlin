// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +InferThrowableTypeParameterToUpperBound

class InvokeAndFun {
    companion object {
        operator fun invoke(block: () -> String): String = "invoke"
    }
}

fun InvokeAndFun(block: () -> Unit): Int = 1

fun testInvokeAndFun() {
    val stringResult = InvokeAndFun { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>stringResult<!>

    val unitResult = InvokeAndFun { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>unitResult<!>

    val nothingResult = InvokeAndFun { TODO() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>nothingResult<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, functionalType, integerLiteral,
lambdaLiteral, localProperty, objectDeclaration, operator, propertyDeclaration, stringLiteral */

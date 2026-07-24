// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +InferThrowableTypeParameterToUpperBound, +ContextParameters, +ExplicitContextArguments

context(a: () -> String)
fun namedContext(): Int = 1

context(a: () -> Unit)
fun namedContext(): String = "(2)"

fun testNamedContext() {
    val stringResult = namedContext(a = { "" })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>stringResult<!>

    val unitResult = namedContext(a = { Unit })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>unitResult<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionDeclarationWithContext, functionalType, integerLiteral,
lambdaLiteral, localProperty, propertyDeclaration, stringLiteral */

// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +InferThrowableTypeParameterToUpperBound, +ContextParameters, +ExplicitContextArguments

fun a(block: () -> Unit) = 1
fun a(block: Sam) = "2"

fun interface Sam {
    fun run(): String
}

fun test() {
    val result = a {
        run { <!RETURN_TYPE_MISMATCH!>{ 1 }<!> }
    };
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>result<!>
}

/* GENERATED_FIR_TAGS: funInterface, functionDeclaration, functionalType, integerLiteral, interfaceDeclaration,
lambdaLiteral, stringLiteral */

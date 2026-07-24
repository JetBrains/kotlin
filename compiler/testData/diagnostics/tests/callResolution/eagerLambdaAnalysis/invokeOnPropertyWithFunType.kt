// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +InferThrowableTypeParameterToUpperBound

fun invokeOnPropertyWithFunType(a: () -> Unit): Int = 1

val invokeOnPropertyWithFunType: (() -> String) -> Unit = {  }

fun test() {
    val result = invokeOnPropertyWithFunType { " " }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>result<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, lambdaLiteral, localProperty,
propertyDeclaration, stringLiteral */

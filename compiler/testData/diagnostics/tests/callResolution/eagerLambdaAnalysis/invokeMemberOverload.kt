// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +InferThrowableTypeParameterToUpperBound

class A {
    operator fun invoke(block: () -> Unit): Int = 1
    operator fun invoke(block: () -> String): String = "(2)"
}

fun test(a: A) {
    val stringResult = a { "string" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>stringResult<!>

    val unitResult = a { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>unitResult<!>

    val nothingResult = <!OVERLOAD_RESOLUTION_AMBIGUITY!>a<!> { TODO() }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, integerLiteral, lambdaLiteral,
localProperty, operator, propertyDeclaration, stringLiteral */

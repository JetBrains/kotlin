// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +InferThrowableTypeParameterToUpperBound

fun multipleOverloadsWithMultipleArguments(first: () -> String, second: () -> Number): Int = 1

fun multipleOverloadsWithMultipleArguments(first: () -> CharSequence, second: () -> Int): String = "(2)"

fun multipleOverloadsWithMultipleArguments(first: () -> Unit, second: () -> Unit): Number = 3.0

fun testCompetingLambdaPositions() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>multipleOverloadsWithMultipleArguments<!>({ "OK" }) { 1 }
}

fun test() {
    val result1 = multipleOverloadsWithMultipleArguments({ "OK" }) { 1.0 }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>result1<!>

    val result2 = multipleOverloadsWithMultipleArguments({ StringBuilder() }) { 1 }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>result2<!>

    val result3 = multipleOverloadsWithMultipleArguments({ Unit }) { 1 }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>result3<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, lambdaLiteral, localProperty,
propertyDeclaration, stringLiteral */

// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +InferThrowableTypeParameterToUpperBound

fun multipleOverloadsWithGeneric(block: () -> String): Int = 1

fun <T> multipleOverloadsWithGeneric(block: () -> T): String = "(2)"

fun multipleOverloadsWithGeneric(block: () -> Unit): Number = 3.0

fun testStringOverload() {
    val result = multipleOverloadsWithGeneric { "OK" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>result<!>
}

fun testGenericOverload() {
    val result = multipleOverloadsWithGeneric { 1 }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>result<!>
}

fun testUnitOverload() {
    val result = multipleOverloadsWithGeneric { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>result<!>
}

fun testNothingOverload() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>multipleOverloadsWithGeneric<!> { TODO() }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, lambdaLiteral, localProperty, nullableType,
propertyDeclaration, stringLiteral, typeParameter */

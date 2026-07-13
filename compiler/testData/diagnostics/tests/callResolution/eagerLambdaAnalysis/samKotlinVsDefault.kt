// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +InferThrowableTypeParameterToUpperBound

fun interface StringProducer {
    fun produce(): String
}

fun interface UnitProducer {
    fun produce(): Unit
}

fun stringSamWithDefaultOrUnitLambda(x: Int = 0, block: StringProducer): Int = 1
fun stringSamWithDefaultOrUnitLambda(block: () -> Unit): String = "(2)"

fun unitSamWithDefaultOrStringLambda(x: Int = 0, block: UnitProducer): Int = 1
fun unitSamWithDefaultOrStringLambda(block: () -> String): String = "(2)"

fun stringSamOrUnitLambdaWithDefault(x: Int = 0, block: () -> Unit): Int = 1
fun stringSamOrUnitLambdaWithDefault(block: StringProducer): String = "(2)"

fun unitSamOrStringLambdaWithDefault(x: Int = 0, block: () -> String): Int = 1
fun unitSamOrStringLambdaWithDefault(block: UnitProducer): String = "(2)"

fun testStringSamWithDefaultOrUnitLambda() {
    val stringResult = stringSamWithDefaultOrUnitLambda { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>stringResult<!>

    val explicitUnitResult = stringSamWithDefaultOrUnitLambda { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>explicitUnitResult<!>

    val nothingResult = stringSamWithDefaultOrUnitLambda { TODO() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>nothingResult<!>

    val coercedUnitResult = stringSamWithDefaultOrUnitLambda { 1 }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>coercedUnitResult<!>
}

fun testUnitSamWithDefaultOrStringLambda() {
    val stringResult = unitSamWithDefaultOrStringLambda { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>stringResult<!>

    val explicitUnitResult = unitSamWithDefaultOrStringLambda { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>explicitUnitResult<!>

    val nothingResult = unitSamWithDefaultOrStringLambda { TODO() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>nothingResult<!>

    val coercedUnitResult = unitSamWithDefaultOrStringLambda { 1 }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>coercedUnitResult<!>
}

fun testStringSamOrUnitLambdaWithDefault() {
    val stringResult = stringSamOrUnitLambdaWithDefault { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>stringResult<!>

    val explicitUnitResult = stringSamOrUnitLambdaWithDefault { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>explicitUnitResult<!>

    val coercedUnitResult = stringSamOrUnitLambdaWithDefault { 1 }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>coercedUnitResult<!>

    val namedBlockResult = stringSamOrUnitLambdaWithDefault(block = { "" })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>namedBlockResult<!>
}

fun testUnitSamOrStringLambdaWithDefault() {
    val stringResult = unitSamOrStringLambdaWithDefault { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>stringResult<!>

    val namedBlockResult = unitSamOrStringLambdaWithDefault(block = { "" })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>namedBlockResult<!>
}

/* GENERATED_FIR_TAGS: funInterface, functionDeclaration, functionalType, integerLiteral, interfaceDeclaration,
lambdaLiteral, localProperty, propertyDeclaration, samConversion, stringLiteral */

// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +InferThrowableTypeParameterToUpperBound

fun stringWithDefaultOrUnit(block: () -> String, x: Int = 0) = 1
fun stringWithDefaultOrUnit(block: () -> Unit) = "(2)"

fun stringOrUnitWithDefault(block: () -> String) = 1
fun stringOrUnitWithDefault(block: () -> Unit, x: Int = 0) = "(2)"

fun testStringWithDefaultOrUnit() {
    val stringResult = stringWithDefaultOrUnit(block = { "" })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>stringResult<!>

    val explicitUnitResult = stringWithDefaultOrUnit(block = { Unit })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>explicitUnitResult<!>

    val coercedUnitResult = stringWithDefaultOrUnit(block = { 1 })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>coercedUnitResult<!>

    <!NONE_APPLICABLE!>stringWithDefaultOrUnit<!>(x = 1) { "" }
}

fun testStringOrUnitWithDefault() {
    val stringResult = stringOrUnitWithDefault(block = { "" })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>stringResult<!>

    val explicitUnitResult = stringOrUnitWithDefault(block = { Unit })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>explicitUnitResult<!>

    val coercedUnitResult = stringOrUnitWithDefault(block = { 1 })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>coercedUnitResult<!>

    <!NONE_APPLICABLE!>stringOrUnitWithDefault<!>(x = 1) { "" }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, lambdaLiteral, localProperty,
propertyDeclaration, stringLiteral */

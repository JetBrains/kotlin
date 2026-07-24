// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +InferThrowableTypeParameterToUpperBound

object Type

infix fun String.lambdaInArgument(block: () -> Type): Int = 1
infix fun String.lambdaInArgument(block: () -> Unit): String = "(2)"

infix fun (() -> Type).lambdaInReceinverAndArgument(block: () -> Unit): Int = 1
infix fun (() -> Unit).lambdaInReceinverAndArgument(block: () -> Type): String = "(2)"

infix fun (() -> Unit).lambdaWithArgument(block: (String) -> Unit): Int = 1
infix fun (() -> Int).lambdaWithArgument(block: (Int) -> Unit): String = "(2)"

fun testLambdaInArgument() {
    val typeResult = "OK" lambdaInArgument { Type }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>typeResult<!>

    val explicitUnitResult = "OK" lambdaInArgument { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>explicitUnitResult<!>

    val coercedUnitResult = "OK" lambdaInArgument { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>coercedUnitResult<!>

    "OK" <!OVERLOAD_RESOLUTION_AMBIGUITY!>lambdaInArgument<!> { TODO() }
}

fun testLambdaInReceinverAndArgument() {
    val receiverTypeResult = { Type } lambdaInReceinverAndArgument { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>receiverTypeResult<!>

    val receiverUnitResult = { Unit } lambdaInReceinverAndArgument { Type }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>receiverUnitResult<!>

    val argumentTypeResult = { Type } lambdaInReceinverAndArgument { Type }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>argumentTypeResult<!>

    { Unit } lambdaInReceinverAndArgument { <!RETURN_TYPE_MISMATCH!>Unit<!> }

    val coercedReceiverResult = { 1 } <!NONE_APPLICABLE!>lambdaInReceinverAndArgument<!> { Type }
    <!DEBUG_INFO_EXPRESSION_TYPE("ERROR CLASS: Ambiguity: lambdaInReceinverAndArgument, [/lambdaInReceinverAndArgument, /lambdaInReceinverAndArgument]")!>coercedReceiverResult<!>

    { 1 } <!NONE_APPLICABLE!>lambdaInReceinverAndArgument<!> { 1 }

    { TODO() } <!OVERLOAD_RESOLUTION_AMBIGUITY!>lambdaInReceinverAndArgument<!> { TODO() }
}

fun testLambdaWithArgument() {
    val result = { 1 } lambdaWithArgument { <!EXPECTED_PARAMETER_TYPE_MISMATCH!>value: String<!> -> value.length }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>result<!>
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, functionalType, infix, integerLiteral,
lambdaLiteral, localProperty, objectDeclaration, propertyDeclaration, stringLiteral */

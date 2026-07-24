// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +InferThrowableTypeParameterToUpperBound

fun unitLambdaOrNullableStringLambda(block: () -> Unit): Int = 1
fun unitLambdaOrNullableStringLambda(block: () -> String?): String = "(2)"

fun nullableUnitLambdaOrStringLambda(block: () -> Unit?): Int = 1
fun nullableUnitLambdaOrStringLambda(block: () -> String): String = "(2)"

fun nullableUnitLambdaOrUnitLambda(block: () -> Unit?): Int = 1
fun nullableUnitLambdaOrUnitLambda(block: () -> Unit): String = "(2)"

val flag = true

fun test1() {
    val nullResult = unitLambdaOrNullableStringLambda { null }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>nullResult<!>

    val nullableStringResult = unitLambdaOrNullableStringLambda { if (flag) "" else null }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>nullableStringResult<!>

    val unitResult = unitLambdaOrNullableStringLambda { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>unitResult<!>

    val coercedUnitResult = unitLambdaOrNullableStringLambda { 1 }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>coercedUnitResult<!>

    <!OVERLOAD_RESOLUTION_AMBIGUITY!>unitLambdaOrNullableStringLambda<!> { TODO() }
}

fun test2() {
    val nullResult = nullableUnitLambdaOrStringLambda { null }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>nullResult<!>

    nullableUnitLambdaOrStringLambda { <!RETURN_TYPE_MISMATCH!>if (flag) "" else null<!> }

    val unitResult = nullableUnitLambdaOrStringLambda { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>unitResult<!>

    nullableUnitLambdaOrStringLambda { <!RETURN_TYPE_MISMATCH!>1<!> }

    <!OVERLOAD_RESOLUTION_AMBIGUITY!>nullableUnitLambdaOrStringLambda<!> { TODO() }
}

fun test3() {
    val nullResult = nullableUnitLambdaOrUnitLambda { null }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>nullResult<!>

    val nullableStringResult = nullableUnitLambdaOrUnitLambda { if (flag) "" else null }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>nullableStringResult<!>

    val unitResult = nullableUnitLambdaOrUnitLambda { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>unitResult<!>

    val coercedUnitResult = nullableUnitLambdaOrUnitLambda { 1 }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>coercedUnitResult<!>

    nullableUnitLambdaOrUnitLambda { TODO() }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, ifExpression, integerLiteral, lambdaLiteral, localProperty,
nullableType, propertyDeclaration, stringLiteral */

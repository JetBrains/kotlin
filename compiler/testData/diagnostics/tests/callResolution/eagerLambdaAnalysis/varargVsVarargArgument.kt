// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +InferThrowableTypeParameterToUpperBound

val flag = true

fun varargAndVararg(vararg blocks: () -> Unit) = 1
fun varargAndVararg(vararg blocks: () -> String) = "(2)"

fun varargAndVarargWithSpecificType(a: Number, vararg blocks: () -> String) = 1
fun varargAndVarargWithSpecificType(a: Int, vararg blocks: () -> Unit) = "(2)"

fun testVarargAndVararg() {
    val stringThenUnit = varargAndVararg({ "" }, { Unit })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>stringThenUnit<!>

    val unitThenString = varargAndVararg({ Unit }, { "" })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>unitThenString<!>

    val coercedThenString = varargAndVararg({ 1 }, { "" })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>coercedThenString<!>

    val stringThenString = varargAndVararg({ "" }, { "" })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>stringThenString<!>

    val unitThenUnit = varargAndVararg({ Unit }, { Unit })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>unitThenUnit<!>

    val stringThenNothing = varargAndVararg({ "" }, { TODO() })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>stringThenNothing<!>

    val stringThenIf = varargAndVararg({ "" }, { if (flag) Unit else "" })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>stringThenIf<!>
}

fun testVarargAndVarargWithSpecificType() {
    val stringResult = varargAndVarargWithSpecificType(1, { "" })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>stringResult<!>

    val explicitUnitResult = varargAndVarargWithSpecificType(1, { Unit })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>explicitUnitResult<!>

    val coercedUnitResult = varargAndVarargWithSpecificType(1, { 42 })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>coercedUnitResult<!>

    val multipleStringResult = varargAndVarargWithSpecificType(1, { "" }, { "" })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>multipleStringResult<!>

    val mixedResult = varargAndVarargWithSpecificType(1, { "" }, { Unit })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>mixedResult<!>

    val numberAndStringResult = varargAndVarargWithSpecificType(1.0, { "" })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>numberAndStringResult<!>

    val numberAndUnitResult = varargAndVarargWithSpecificType(1.0, { <!RETURN_TYPE_MISMATCH!>Unit<!> })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>numberAndUnitResult<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, ifExpression, integerLiteral, lambdaLiteral, localProperty,
propertyDeclaration, stringLiteral, vararg */

// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +InferThrowableTypeParameterToUpperBound

fun unitLambdaOrVarargString(blocks: () -> Unit) = 1
fun unitLambdaOrVarargString(vararg blocks: () -> String) = "(2)"

fun stringLambdaOrVarargUnit(blocks: () -> String) = 1
fun stringLambdaOrVarargUnit(vararg blocks: () -> Unit) = "(2)"

fun specificTypeAndVararg(a: Number, block: () -> String) = 1
fun specificTypeAndVararg(a: Int, vararg blocks: () -> Unit) = "(2)"

fun testUnitLambdaOrVarargStringAsRegularArgument() {
    val stringResult = unitLambdaOrVarargString({ "" })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>stringResult<!>

    val explicitUnitResult = unitLambdaOrVarargString({ Unit })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>explicitUnitResult<!>

    val coercedUnitResult = unitLambdaOrVarargString({ 1 })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>coercedUnitResult<!>

    val emptyVarargResult = unitLambdaOrVarargString()
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>emptyVarargResult<!>
}

fun testUnitLambdaOrVarargStringAsTrailingLambda() {
    val stringResult = unitLambdaOrVarargString { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>stringResult<!>

    val explicitUnitResult = unitLambdaOrVarargString { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>explicitUnitResult<!>

    val nothingResult = unitLambdaOrVarargString { TODO() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>nothingResult<!>

    val coercedUnitResult = unitLambdaOrVarargString { 1 }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>coercedUnitResult<!>
}

fun testUnitLambdaOrVarargStringWithMultipleArguments() {
    val stringResult = unitLambdaOrVarargString({ "" }, { "" })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>stringResult<!>
}

fun testStringLambdaOrVarargUnitAsRegularArgument() {
    val stringResult = stringLambdaOrVarargUnit({ "" })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>stringResult<!>

    val explicitUnitResult = stringLambdaOrVarargUnit({ Unit })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>explicitUnitResult<!>

    val coercedUnitResult = stringLambdaOrVarargUnit({ 1 })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>coercedUnitResult<!>

    val ifResult = stringLambdaOrVarargUnit({ if (true) Unit else "1" })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>ifResult<!>

    val emptyVarargResult = stringLambdaOrVarargUnit()
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>emptyVarargResult<!>
}

fun testStringLambdaOrVarargUnitAsTrailingLambda() {
    val stringResult = stringLambdaOrVarargUnit { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>stringResult<!>

    val nothingResult = stringLambdaOrVarargUnit { TODO() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>nothingResult<!>
}

fun testStringLambdaOrVarargUnitWithMultipleArguments() {
    val stringResult = stringLambdaOrVarargUnit({ "" }, { "" })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>stringResult<!>

    val explicitUnitResult = stringLambdaOrVarargUnit({ Unit }, { Unit })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>explicitUnitResult<!>

    val coercedUnitResult = stringLambdaOrVarargUnit({ 1 }, { 1 })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>coercedUnitResult<!>

    val mixedResult = stringLambdaOrVarargUnit({ Unit }, { "" })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>mixedResult<!>
}

fun testNormalParameterBeforeVararg() {
    val stringResult = specificTypeAndVararg(1, { "" })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>stringResult<!>

    val explicitUnitResult = specificTypeAndVararg(1, { Unit })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>explicitUnitResult<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, lambdaLiteral, localProperty,
propertyDeclaration, stringLiteral, vararg */

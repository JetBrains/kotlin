// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +InferThrowableTypeParameterToUpperBound

fun <T : () -> String> functionTypeBoundOrUnitLambda(block: T): Int = 1
fun functionTypeBoundOrUnitLambda(block: () -> Unit): String = "(2)"

fun <T : CharSequence> boundOrUnitLambda(block: () -> T): Int = 1
fun boundOrUnitLambda(block: () -> Unit): String = "(2)"

fun <T : CharSequence?> dnnBoundOrUnitLambda(block: () -> (T & Any)): Int = 1
fun dnnBoundOrUnitLambda(block: () -> Unit): String = "(2)"

fun testFunctionTypeBoundOrUnitLambda() {
    val stringResult = functionTypeBoundOrUnitLambda { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>stringResult<!>

    val explicitUnitResult = functionTypeBoundOrUnitLambda { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>explicitUnitResult<!>

    val coercedUnitResult = functionTypeBoundOrUnitLambda { 1 }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>coercedUnitResult<!>

    val nothingResult = functionTypeBoundOrUnitLambda { TODO() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>nothingResult<!>
}

fun testBoundOrUnitLambda() {
    val stringResult = boundOrUnitLambda { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>stringResult<!>

    val explicitUnitResult = boundOrUnitLambda { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>explicitUnitResult<!>

    val coercedUnitResult = boundOrUnitLambda { 1 }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>coercedUnitResult<!>

    val nothingResult = boundOrUnitLambda { TODO() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>nothingResult<!>
}

fun testDnnBoundOrUnitLambda() {
    val stringResult = dnnBoundOrUnitLambda { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>stringResult<!>

    val explicitUnitResult = dnnBoundOrUnitLambda { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>explicitUnitResult<!>

    val coercedUnitResult = dnnBoundOrUnitLambda { 1 }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>coercedUnitResult<!>

    val nothingResult = dnnBoundOrUnitLambda { TODO() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>nothingResult<!>
}

/* GENERATED_FIR_TAGS: dnnType, functionDeclaration, functionalType, integerLiteral, lambdaLiteral, localProperty,
nullableType, propertyDeclaration, stringLiteral, typeConstraint, typeParameter */

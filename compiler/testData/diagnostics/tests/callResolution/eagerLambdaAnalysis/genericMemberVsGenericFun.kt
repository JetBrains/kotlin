// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +InferThrowableTypeParameterToUpperBound

class GenericMemberAndGenericFunction<T>(val value: T) {
    fun genericMemberVsGenericFunction(block: () -> T): Int = 1
    fun <K> genericMemberVsGenericFunction(block: () -> K): String = "(2)"

    fun testGenericMemberVsGenericFunction() {
        val stringResult = genericMemberVsGenericFunction { " " }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>stringResult<!>

        val valueResult = genericMemberVsGenericFunction { value }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>valueResult<!>

        val nothingResult = genericMemberVsGenericFunction { TODO() }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>nothingResult<!>
    }
}

fun testUnitReceiver() {
    val explicitUnitResult = GenericMemberAndGenericFunction(Unit).genericMemberVsGenericFunction { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>explicitUnitResult<!>

    val stringResult = GenericMemberAndGenericFunction(Unit).genericMemberVsGenericFunction { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>stringResult<!>
}

fun testStringReceiver() {
    val stringResult = GenericMemberAndGenericFunction("").genericMemberVsGenericFunction { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>stringResult<!>

    val nothingResult = GenericMemberAndGenericFunction("").genericMemberVsGenericFunction { TODO() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>nothingResult<!>

    val explicitUnitResult = GenericMemberAndGenericFunction("").genericMemberVsGenericFunction { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>explicitUnitResult<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, integerLiteral, lambdaLiteral,
localProperty, nullableType, primaryConstructor, propertyDeclaration, stringLiteral, typeParameter */

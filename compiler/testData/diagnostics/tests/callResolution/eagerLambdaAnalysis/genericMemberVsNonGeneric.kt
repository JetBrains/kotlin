// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +InferThrowableTypeParameterToUpperBound

class GenericMemberBox<T>(val value: T) {
    fun genericMemberOrUnitLambda(block: () -> T): Int = 1
    fun genericMemberOrUnitLambda(block: () -> Unit): String = "(2)"

    fun genericMemberOrStringLambda(block: () -> T): Int = 1
    fun genericMemberOrStringLambda(block: () -> String): String = "(2)"

    fun testGenericMemberOrUnitLambda() {
        val stringResult = genericMemberOrUnitLambda { "" }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>stringResult<!>

        val explicitUnitResult = genericMemberOrUnitLambda { Unit }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>explicitUnitResult<!>

        val coercedUnitResult = genericMemberOrUnitLambda { 1 }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>coercedUnitResult<!>

        <!OVERLOAD_RESOLUTION_AMBIGUITY!>genericMemberOrUnitLambda<!> { TODO() }

        val declarationResult = genericMemberOrUnitLambda { val x = 10 }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>declarationResult<!>

        val valueTResult = genericMemberOrUnitLambda { value }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>valueTResult<!>
    }

    fun testGenericMemberOrStringLambda() {
        val stringResult = genericMemberOrStringLambda { "" }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>stringResult<!>

        genericMemberOrStringLambda { <!RETURN_TYPE_MISMATCH!>Unit<!> }
        genericMemberOrStringLambda { <!RETURN_TYPE_MISMATCH!>1<!> }
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>genericMemberOrStringLambda<!> { TODO() }
        genericMemberOrStringLambda { <!RETURN_TYPE_MISMATCH!>val x = 10<!> }

        val valueTResult = genericMemberOrStringLambda { value }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>valueTResult<!>
    }
}

fun testStringReceiver() {
    val stringResult = GenericMemberBox("").genericMemberOrUnitLambda { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>stringResult<!>

    val explicitUnitResult = GenericMemberBox("").genericMemberOrUnitLambda { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>explicitUnitResult<!>

    val coercedUnitResult = GenericMemberBox("").genericMemberOrUnitLambda { 1 }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>coercedUnitResult<!>

    GenericMemberBox("").<!OVERLOAD_RESOLUTION_AMBIGUITY!>genericMemberOrUnitLambda<!> { TODO() }
}

fun testUnitReceiver() {
    val stringResult = GenericMemberBox(Unit).genericMemberOrStringLambda { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>stringResult<!>

    val explicitUnitResult = GenericMemberBox(Unit).genericMemberOrStringLambda { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>explicitUnitResult<!>

    val coercedUnitResult = GenericMemberBox(Unit).genericMemberOrStringLambda { 1 }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>coercedUnitResult<!>

    GenericMemberBox(Unit).<!OVERLOAD_RESOLUTION_AMBIGUITY!>genericMemberOrStringLambda<!> { TODO() }
}

fun testInstantiatedOverloadsWithSameFunctionalType() {
    GenericMemberBox(Unit).<!OVERLOAD_RESOLUTION_AMBIGUITY!>genericMemberOrUnitLambda<!> { Unit }
    GenericMemberBox("").<!OVERLOAD_RESOLUTION_AMBIGUITY!>genericMemberOrStringLambda<!> { "" }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, integerLiteral, lambdaLiteral,
localProperty, nullableType, primaryConstructor, propertyDeclaration, stringLiteral, typeParameter */

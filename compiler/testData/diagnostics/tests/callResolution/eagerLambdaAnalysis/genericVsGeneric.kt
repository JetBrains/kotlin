// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +InferThrowableTypeParameterToUpperBound

fun <T> genericUnitOrGenericT(block: () -> Unit): Int = 1
fun <T> genericUnitOrGenericT(block: () -> T): String = "(2)"

fun <T : CharSequence> genericUnitOrBoundedGenericT(block: () -> T): Int = 1
fun <T> genericUnitOrBoundedGenericT(block: () -> Unit): String = "(2)"

fun <T> genericUnitOrGenericWithDefault(x: Int = 1, block: () -> T): Int = 1
fun <T> genericUnitOrGenericWithDefault(block: () -> Unit): String = "(2)"

fun <T : CharSequence> genericTOrBoundenGeneric(block: () -> T): Int = 1
fun <T> genericTOrBoundenGeneric(block: () -> T): String = "(2)"

fun testGenericUnitOrGenericT() {
    val stringResult = genericUnitOrGenericT { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>stringResult<!>

    <!CANNOT_INFER_PARAMETER_TYPE!>genericUnitOrGenericT<!> { TODO() }

    val explicitStringAndNothing = genericUnitOrGenericT<String> { TODO() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>explicitStringAndNothing<!>

    val explicitStringAndString = genericUnitOrGenericT<String> { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>explicitStringAndString<!>

    val explicitStringAndUnit = genericUnitOrGenericT<String> { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>explicitStringAndUnit<!>

    val explicitUnitAndString = genericUnitOrGenericT<Unit> { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>explicitUnitAndString<!>
}

fun testGenericUnitOrBoundedGenericT() {
    val stringResult = genericUnitOrBoundedGenericT { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>stringResult<!>

}

fun testGenericUnitOrGenericWithDefault() {
    val trailingLambdaResult = genericUnitOrGenericWithDefault { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>trailingLambdaResult<!>

    <!CANNOT_INFER_PARAMETER_TYPE!>genericUnitOrGenericWithDefault<!>({ "" })

    val namedBlockResult = genericUnitOrGenericWithDefault(block = { "" })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>namedBlockResult<!>

    <!CANNOT_INFER_PARAMETER_TYPE!>genericUnitOrGenericWithDefault<!>(block = { Unit })
    <!CANNOT_INFER_PARAMETER_TYPE!>genericUnitOrGenericWithDefault<!>({ Unit })
}

fun testGenericTOrBoundenGeneric() {
    val stringResult = genericTOrBoundenGeneric { " " }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>stringResult<!>

    val explicitUnitResult = genericTOrBoundenGeneric { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>explicitUnitResult<!>

    val intResult = genericTOrBoundenGeneric { 1 }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>intResult<!>

    val nothingResult = genericTOrBoundenGeneric { TODO() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>nothingResult<!>

     val explicitStringAndNothing = genericTOrBoundenGeneric<String> { TODO() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>explicitStringAndNothing<!>

    val explicitStringAndString = genericTOrBoundenGeneric<String> { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>explicitStringAndString<!>

    val explicitUnitAndString = genericTOrBoundenGeneric<Unit> { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>explicitUnitAndString<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, lambdaLiteral, localProperty, nullableType,
propertyDeclaration, stringLiteral, typeConstraint, typeParameter */

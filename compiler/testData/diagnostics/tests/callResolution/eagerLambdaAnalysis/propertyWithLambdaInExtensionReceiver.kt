// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +InferThrowableTypeParameterToUpperBound

val (() -> String).propertyWithLambdaReceiver: Int
    get() = 1

val (() -> Unit).propertyWithLambdaReceiver: String
    get() = "2"

fun test() {
    val result = with({ "" }) { propertyWithLambdaReceiver }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>result<!>

    val result2 = { "" }.propertyWithLambdaReceiver
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>result2<!>

    val result3 = with({ Unit }) { propertyWithLambdaReceiver }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>result3<!>

    val result4 = { Unit }.propertyWithLambdaReceiver
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>result4<!>

    val result5 = <!CANNOT_INFER_PARAMETER_TYPE!>with<!>({ 8 }) { <!NONE_APPLICABLE!>propertyWithLambdaReceiver<!> }

    val result6 = { 8 }.<!NONE_APPLICABLE!>propertyWithLambdaReceiver<!>
}


fun testWithNothing() {
    <!CANNOT_INFER_PARAMETER_TYPE!>with<!>({ TODO() }) {
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>propertyWithLambdaReceiver<!>
    };

    { TODO() }.<!OVERLOAD_RESOLUTION_AMBIGUITY!>propertyWithLambdaReceiver<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, getter, integerLiteral, lambdaLiteral, localProperty,
propertyDeclaration, propertyWithExtensionReceiver, stringLiteral */

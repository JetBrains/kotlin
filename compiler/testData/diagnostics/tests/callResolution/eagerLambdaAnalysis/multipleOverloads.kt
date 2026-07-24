// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +InferThrowableTypeParameterToUpperBound

fun foo(a: ()-> String): Int = 1

fun foo(a: ()-> CharSequence): String = "(2)"

fun foo(a: ()-> Unit): Number = 3.0

fun test() {
    val stringResult = foo { "OK" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>stringResult<!>

    val stringBuilderResult = foo { StringBuilder() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>stringBuilderResult<!>

    val unitResult = foo { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>unitResult<!>

    val intResult = foo { 1 }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Number")!>intResult<!>

    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!> { TODO() }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, lambdaLiteral, localProperty,
propertyDeclaration, stringLiteral */

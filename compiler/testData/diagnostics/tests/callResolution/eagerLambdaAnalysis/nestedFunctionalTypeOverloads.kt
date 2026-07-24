// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +InferThrowableTypeParameterToUpperBound

fun nestedFunctionalType(block: () -> (() -> Unit)) = 1
fun nestedFunctionalType(block: () -> (() -> String))= "(2)"

fun returnString(): String = ""
fun returnInt(): Int = 1
fun returnUnit(): Unit = Unit
fun returnNothing(): Nothing = TODO()

fun overloadedReturnSomething(): String = ""
fun overloadedReturnSomething(i: Int): Int = i

fun testNestedFunctionalTypeOverloads() {
    val stringLambdaResult = nestedFunctionalType { { "" } }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>stringLambdaResult<!>

    val unitLambdaResult = nestedFunctionalType { { Unit } }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>unitLambdaResult<!>

    val intLambdaResult = nestedFunctionalType { { 1 } }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>intLambdaResult<!>

    <!OVERLOAD_RESOLUTION_AMBIGUITY!>nestedFunctionalType<!> { { TODO() } }

    <!OVERLOAD_RESOLUTION_AMBIGUITY!>nestedFunctionalType<!> { ::returnString }
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>nestedFunctionalType<!> { ::returnInt }
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>nestedFunctionalType<!> { ::returnUnit }
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>nestedFunctionalType<!> { ::returnNothing }
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>nestedFunctionalType<!> { ::<!OVERLOAD_RESOLUTION_AMBIGUITY!>overloadedReturnSomething<!> }
}

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, functionalType, integerLiteral, lambdaLiteral,
stringLiteral */

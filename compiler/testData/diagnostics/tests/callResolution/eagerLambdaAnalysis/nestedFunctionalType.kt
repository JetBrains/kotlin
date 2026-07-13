// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +InferThrowableTypeParameterToUpperBound

fun nestedFunctionalType(block: () -> Unit): Int = 1

fun nestedFunctionalType(block: () -> (() -> String)): String = "(2)"

fun returnString(): String = ""
fun returnInt(): Int = 1
fun returnUnit(): Unit = Unit
fun returnNothing(): Nothing = TODO()

fun overloadedReturnSomething(): String = ""
fun overloadedReturnSomething(i: Int): Int = i

fun testNestedFunctionalType() {
    val nestedStringLambdaResult = nestedFunctionalType { { "" } }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>nestedStringLambdaResult<!>

    val nestedUnitLambdaResult = nestedFunctionalType { { <!RETURN_TYPE_MISMATCH!>Unit<!> } }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>nestedUnitLambdaResult<!>

    val nestedIntLambdaResult = nestedFunctionalType { { <!RETURN_TYPE_MISMATCH!>1<!> } }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>nestedIntLambdaResult<!>

    val nestedNothingLambdaResult = nestedFunctionalType { { TODO() } }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>nestedNothingLambdaResult<!>

    val nestedObjectResult = nestedFunctionalType {
        object : () -> String {
            override fun invoke() = ""
        }
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>nestedObjectResult<!>

    val stringResult = nestedFunctionalType { ::returnString }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>stringResult<!>

    nestedFunctionalType { ::<!INAPPLICABLE_CANDIDATE!>returnInt<!> }

    val unitResult = nestedFunctionalType { ::<!INAPPLICABLE_CANDIDATE!>returnUnit<!> }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>unitResult<!>

    val nothingResult = nestedFunctionalType { ::returnNothing }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>nothingResult<!>

    val somethingResult = nestedFunctionalType { ::overloadedReturnSomething }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>somethingResult<!>
}

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, functionalType, integerLiteral, lambdaLiteral,
localProperty, propertyDeclaration, stringLiteral */

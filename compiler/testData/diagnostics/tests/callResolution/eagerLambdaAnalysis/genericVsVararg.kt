// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +InferThrowableTypeParameterToUpperBound

fun <T> foo(a: () -> T, b: Int = 1): Int = 1
fun foo(a: () -> Unit, vararg b: Int): String = "(2)"

fun test() {
    val stringResult = foo ({ " " })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>stringResult<!>

    val unitResult = foo ({ Unit })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>unitResult<!>

    val nothingResult = foo ({ TODO() })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>nothingResult<!>

    val unitAndIntResult = foo ({ Unit }, 0)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>unitAndIntResult<!>

    val stringAndIntResult = foo ({ "" }, 0)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>stringAndIntResult<!>

    val stringAndVarargResult = foo ({ "" }, 0, 3)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>stringAndVarargResult<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, lambdaLiteral, localProperty, nullableType,
propertyDeclaration, stringLiteral, typeParameter, vararg */

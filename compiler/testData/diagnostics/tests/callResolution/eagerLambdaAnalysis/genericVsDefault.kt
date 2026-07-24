// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +InferThrowableTypeParameterToUpperBound

fun <T> foo(a: () -> T): Int = 1
fun foo(a: ()-> Unit, b: Int = 1): String = "(2)"

fun <T> bar(a: () -> T): Int = 1
fun bar(a: ()-> String, b: Int = 1): String = "(2)"

fun baz(a: Int = 1, b: () -> Unit): Int = 1
fun <T> baz(b: () -> T): String = "(2)"

fun qux(a: Int = 1, b: () -> String): Int = 1
fun <T> qux(b: () -> T): String = "(2)"

fun test1() {
    val result1 = foo { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>result1<!>

    val result2 = foo ({ "" })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>result2<!>
}

fun test2() {
    val result1 = bar { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>result1<!>

    val result2 = bar ({ "" })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>result2<!>

    val result3 = bar ({ Unit })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>result3<!>

    val result4 = bar { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>result4<!>
}

fun test3() {
    val result1 = baz { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>result1<!>

    val result2 = baz ({ "" })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>result2<!>
}

fun test4_2() {
    val result1 = qux { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>result1<!>

    val result2 = qux ({ "" })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>result2<!>

    val result3 = qux ({ Unit })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>result3<!>

    val result4 = qux { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>result4<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, lambdaLiteral, localProperty, nullableType,
propertyDeclaration, stringLiteral, typeParameter */

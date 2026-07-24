// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +InferThrowableTypeParameterToUpperBound
// ISSUE: KT-86214

fun <T> foo(param: () -> T): Int = 1
fun foo(param: suspend () -> Unit): String = "(2)"

fun <T> bar(param: suspend () -> T): Int = 1
fun bar(param: () -> Unit): String = "(2)"

suspend fun suspendStringFun() = ""
suspend fun suspendUnitFun() { }

fun test1() {
    val stringResult = foo { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>stringResult<!>

    val unitResult = foo { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>unitResult<!>

     val suspendStringResult = foo { <!ILLEGAL_SUSPEND_FUNCTION_CALL!>suspendStringFun<!>() }             //KT-86214
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>suspendStringResult<!>

    val suspendUnitResult = foo { suspendUnitFun() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>suspendUnitResult<!>
}

fun test2() {
    val stringResult = bar { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>stringResult<!>

    val unitResult = bar { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>unitResult<!>

    val suspendStringResult = bar { suspendStringFun() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>suspendStringResult<!>

    val suspendUnitResult = bar { <!ILLEGAL_SUSPEND_FUNCTION_CALL!>suspendUnitFun<!>() }                   //KT-86214
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>suspendUnitResult<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, lambdaLiteral, localProperty, nullableType,
propertyDeclaration, stringLiteral, suspend, typeParameter */

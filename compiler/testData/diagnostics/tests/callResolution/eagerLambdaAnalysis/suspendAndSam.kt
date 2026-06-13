// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +UnitConversionsOnArbitraryExpressions, +InferThrowableTypeParameterToUpperBound
// ISSUES: KT-86214
import kotlin.experimental.ExperimentalTypeInference

fun interface StringSam {
    fun run(): String
}

fun interface UnitSam {
    fun run()
}

fun interface SuspendStringSam {
    suspend fun run(): String
}

fun interface SuspendUnitSam {
    suspend fun run()
}

suspend fun suspendFun() = ""
suspend fun suspendUnitFun() { }

fun a(block: suspend () -> Unit) = 1
fun a(block: StringSam) = "(2)"

fun b(block: () -> Unit) = 1
fun b(block: SuspendStringSam) = "(2)"

fun c(block: () -> String) = 1
fun c(block: SuspendUnitSam) = "(2)"

fun d(block: suspend () -> String) = 1
fun d(block: UnitSam) = "(2)"

@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
fun e(block: suspend () -> Unit) = 1
fun e(block: StringSam) = "(2)"

fun testSuspendUnitLambdaAndStringSam() {
    val case1 = a { "OK" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>case1<!>

    val case2 = a { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case2<!>

    val case3 = a { <!ILLEGAL_SUSPEND_FUNCTION_CALL!>suspendFun<!>() }  //KT-86214
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>case3<!>

    val case4 = a { suspendUnitFun() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case4<!>

    val case5 = a { object {} }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case5<!>

    val case6 = a { TODO() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case6<!>
}

fun testUnitLambdaAndSuspendStringSam() {
    val case1 = b { "OK" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>case1<!>

    val case2 = b { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case2<!>

    val case3 = b { suspendFun() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>case3<!>

    val case4 = b { <!ILLEGAL_SUSPEND_FUNCTION_CALL!>suspendUnitFun<!>() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case4<!>

    val case5 = b { object {} }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case5<!>

    val case6 = b { TODO() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case6<!>
}

fun testStringLambdaAndSuspendUnitSam() {
    val case1 = c { "OK" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case1<!>

    val case2 = c { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>case2<!>

    val case3 = c { <!ILLEGAL_SUSPEND_FUNCTION_CALL!>suspendFun<!>() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case3<!>

    val case4 = c { suspendUnitFun() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>case4<!>

    val case5 = c { object {} }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>case5<!>

    val case6 = c { TODO() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case6<!>
}

fun testSuspendStringLambdaAndUnitSam() {
    val case1 = d { "OK" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case1<!>

    val case2 = d { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>case2<!>

    val case3 = d { suspendFun() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case3<!>

    val case4 = d { <!ILLEGAL_SUSPEND_FUNCTION_CALL!>suspendUnitFun<!>() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>case4<!>

    val case5 = d { object {} }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>case5<!>

    val case6 = d { TODO() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case6<!>
}

fun testSuspendAndSamWithAnnotation() {
    val case1 = e { "OK" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>case1<!>

    val case2 = e { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case2<!>

    val case3 = e { <!ILLEGAL_SUSPEND_FUNCTION_CALL!>suspendFun<!>() }  //KT-86214
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>case3<!>

    val case4 = e { suspendUnitFun() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case4<!>

    val case5 = e { object {} }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case5<!>

    val case6 = e { TODO() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case6<!>
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classReference, funInterface, functionDeclaration, functionalType,
integerLiteral, interfaceDeclaration, lambdaLiteral, localProperty, propertyDeclaration, samConversion, stringLiteral,
suspend */

// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +UnitConversionsOnArbitraryExpressions, +InferThrowableTypeParameterToUpperBound
import kotlin.experimental.ExperimentalTypeInference

fun a(block: () -> Unit) = 1
fun a(block: Sam) = "(2)"

fun b(block: () -> Type) = 1
fun b(block: UnitSam) = "(2)"

fun c(block: Sam) = 1
fun c(block: UnitSam) = "(2)"

@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
fun d(block: () -> Unit) = 1
fun d(block: Sam) = "(2)"

object Type

fun interface Sam {
    fun run(): Type
}

fun interface UnitSam {
    fun run()
}

val flag = true

fun testUnitLambdaAndTypeSam() {
    val case1 = a { Type }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>case1<!>

    val case2 = a { Type; return@a }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case2<!>

    val case3 = a { return@a Type }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>case3<!>

    val case4 = a { println(1) }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case4<!>

    val case5 = a { }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case5<!>

    val case6 = a { if (flag) Type else Type }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>case6<!>

    val case7 = a { if (flag) Type }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case7<!>

    val case8 = a { while(true){} }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case8<!>

    val case9 = a { { }() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case9<!>

    val case10 = a { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case10<!>

    val case11 = a { { Type }() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>case11<!>

    val case12 = a { { "" }() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case12<!>
}

fun testTypeLambdaAndUnitSam() {
    val case1 = b { Type }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case1<!>

    val case2 = b { Type; return@b }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>case2<!>

    val case3 = b { return@b Type }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case3<!>

    val case4 = b { println(1) }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>case4<!>

    val case5 = b { }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>case5<!>

    val case6 = b { if (flag) Type else Type }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case6<!>

    val case7 = b { if (flag) Type }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>case7<!>

    val case8 = b { while(true){} }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>case8<!>

    val case9 = b { { }() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>case9<!>

    val case10 = b { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>case10<!>

    val case11 = b { { Type }() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case11<!>

    val case12 = b { { "" }() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>case12<!>
}

fun testTypeSamAndUnitSam() {
    val case1 = c { Type }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case1<!>

    val case2 = c { Type; return@c }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>case2<!>

    val case3 = c { return@c Type }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case3<!>

    val case4 = c { println(1) }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>case4<!>

    val case5 = c { }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>case5<!>

    val case6 = c { if (flag) Type else Type }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case6<!>

    val case7 = c { if (flag) Type }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>case7<!>

    val case8 = c { while(true){} }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>case8<!>

    val case9 = c { { }() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>case9<!>

    val case10 = c { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>case10<!>

    val case11 = c { { Type }() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case11<!>

    val case12 = c { { "" }() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>case12<!>
}

fun testLambdaAndSamWithAnnotation() {
    val case1 = d { Type }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>case1<!>

    val case2 = d { Type; return@d }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case2<!>

    val case3 = d { return@d Type }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>case3<!>

    val case4 = d { println(1) }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case4<!>

    val case5 = d { }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case5<!>

    val case6 = d { if (flag) Type else Type }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>case6<!>

    val case7 = d { if (flag) Type }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case7<!>

    val case8 = d { while(true){} }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case8<!>

    val case9 = d { { }() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case9<!>

    val case10 = d { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case10<!>

    val case11 = d { { Type }() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>case11<!>

    val case12 = d { { "" }() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>case12<!>
}

/* GENERATED_FIR_TAGS: classReference, funInterface, functionDeclaration, functionalType, ifExpression, integerLiteral,
interfaceDeclaration, lambdaLiteral, localProperty, objectDeclaration, propertyDeclaration, samConversion, stringLiteral,
whileLoop */

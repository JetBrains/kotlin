// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// ISSUE: KT-87007
// LANGUAGE: +EagerLambdaAnalysis

fun <T> id(x: T): T = x

fun foo(block: SAM, block2: () -> Int) = 1
fun foo(block: () -> Int, block2: () -> Unit) = "2"

fun interface SAM {
    fun run(): String
}

fun test() {
    val intIntLambda = foo(id { <!RETURN_TYPE_MISMATCH!>1<!> }) { 1 }

    val intUnitLambda = foo(id { 1 }) { Unit }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>intUnitLambda<!>

    val intNothingLambda = foo(id { 1 }) { TODO() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>intNothingLambda<!>
}

/* GENERATED_FIR_TAGS: funInterface, functionDeclaration, functionalType, integerLiteral, interfaceDeclaration,
lambdaLiteral, nullableType, samConversion, stringLiteral, typeParameter */

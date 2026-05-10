// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// ISSUE: KT-85593
// LANGUAGE: +EagerLambdaAnalysis

// Three overloads: first lambda eliminates candidate 1, second lambda eliminates candidate 2, candidate 3 survives
@JvmName("fooIntString")
fun foo(a: () -> Int, b: () -> String): Int = 1
@JvmName("fooStringInt")
fun foo(a: () -> String, b: () -> Int): Double = 2.0
@JvmName("fooStringString")
fun foo(a: () -> String, b: () -> String): String = "stringString"

fun main() {
    // { "" } returns String -> eliminates fooIntString (a: () -> Int mismatch)
    // { "" } returns String -> eliminates fooStringInt (b: () -> Int mismatch)
    // only fooStringString survives
    val x = foo({ "" }) { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>x<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, lambdaLiteral, localProperty,
propertyDeclaration, stringLiteral */

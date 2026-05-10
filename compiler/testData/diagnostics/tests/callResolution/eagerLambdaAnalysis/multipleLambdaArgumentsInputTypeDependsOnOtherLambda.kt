// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// ISSUE: KT-85593
// LANGUAGE: +EagerLambdaAnalysis

@JvmName("foo1")
fun <T1> foo(a: (T1) -> Int, b: () -> T1): Int = 1

@JvmName("foo2")
fun <T2> foo(a: (T2) -> String, b: () -> T2): Double = 2.0

@JvmName("foo3")
fun foo(a: (Int) -> String, b: () -> Int): String = "3"

fun main() {
    // At first stage we remove foo3 because of the second lambda inapplicability
    // Then `foo1` because of the first lambda inapplicability
    val x = foo({ it }) { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Double")!>x<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, lambdaLiteral, localProperty,
propertyDeclaration, stringLiteral */

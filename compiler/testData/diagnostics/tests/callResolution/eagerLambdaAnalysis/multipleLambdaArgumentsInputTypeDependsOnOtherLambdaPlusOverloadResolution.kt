// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// ISSUE: KT-85593
// LANGUAGE: +EagerLambdaAnalysis

@JvmName("foo1")
fun <T1> foo(x: String, a: (T1) -> Int, b: () -> T1): Int = 1

@JvmName("foo2")
fun <T2> foo(x: String, a: (T2) -> String, b: () -> T2): Double = 2.0

@JvmName("foo3")
fun <T3> foo(x: Any, a: (T3) -> Any, b: () -> T3): Char = '3'

@JvmName("foo4")
fun foo(x: String, a: (Int) -> String, b: () -> Int): String = "4"

fun main() {
    // At first stage we remove foo4 because of the second lambda inapplicability
    // Then `foo1` because of the first lambda inapplicability
    // After that, only foo2/foo3 are left among which the foo2 is more specific
    val x = foo("", { it }) { "" }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Double")!>x<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, lambdaLiteral, localProperty,
propertyDeclaration, stringLiteral */

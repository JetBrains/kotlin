// LANGUAGE: +CollectionLiterals +EagerLambdaAnalysis +ContextSensitiveResolutionUsingExpectedType
// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

// FILE: test2/main.kt
package test2

fun <D: Collection<E>, E> foo(d: D, lam: (E) -> Int): D= d
@JvmName("foo2")
fun <D: Collection<E>, E> foo(d: D, lam: (E) -> String): D = d
fun <D: Collection<E>, E> bar(d: D, lam: (E) -> Int): D = d

fun test() {
    val x: Set<Int> <!INITIALIZER_TYPE_MISMATCH!>=<!> foo([1, 2, 3]) { it }
    val y: Set<Int> = bar([1, 2, 3]) { it }
}


/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, lambdaLiteral, stringLiteral */

// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// FIR_DUMP
// LANGUAGE: +EagerLambdaAnalysis
// ISSUE: KT-51401

fun foo(x: Int): Int = 1
fun foo(x: Float): Int = 1

fun bar(): Int = 1

fun <K, L> K.debounce(cr: (L) -> K, cr2: () -> L, timeoutMillis: (K) -> Int): K = TODO()

@JvmName("debounceDuration")
fun <K, L> K.debounce(cr: (L) -> K, cr2: () -> L, timeout: (K) -> String): K = TODO()

fun main(x: Int) {
    x.<!OVERLOAD_RESOLUTION_AMBIGUITY!>debounce<!>(::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>, ::bar) { <!CANNOT_INFER_VALUE_PARAMETER_TYPE!>value<!> -> 0 }
}

/* GENERATED_FIR_TAGS: callableReference, funWithExtensionReceiver, functionDeclaration, functionalType, integerLiteral,
lambdaLiteral, nullableType, stringLiteral, thisExpression, typeParameter */

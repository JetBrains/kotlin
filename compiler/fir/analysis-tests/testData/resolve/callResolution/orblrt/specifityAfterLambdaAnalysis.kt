// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-42648
// WITH_STDLIB

@JvmName("takeLambdaNullable")
fun takeLambda(block: () -> Double) { // (1)
    TODO()
}

fun takeLambda(block: () -> Double?) { // (2)
    TODO()
}

fun main() {
    takeLambda { 1.0 } // OK (1)
    takeLambda { null } // Fail (1)
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, lambdaLiteral, nullableType, stringLiteral */

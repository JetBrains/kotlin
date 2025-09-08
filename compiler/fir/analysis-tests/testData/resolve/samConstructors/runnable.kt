// RUN_PIPELINE_TILL: BACKEND
fun foo(runnable: Runnable) {}

fun main() {
    foo(Runnable {})
}

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral */

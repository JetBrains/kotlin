// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +BreakContinueInInlineLambdas
// WITH_STDLIB
// ISSUE: KT-68277

fun main() {
    while(true) {
        run {
            break
        }
    }
    println("hi!")
}

/* GENERATED_FIR_TAGS: break, functionDeclaration, lambdaLiteral, stringLiteral, whileLoop */

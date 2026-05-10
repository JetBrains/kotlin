// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis
// ISSUE: KT-73197


fun submit1(x: () -> Unit) {}
fun submit1(x: () -> String): String = ""

fun submit2(x: () -> String): String = ""
fun submit2(x: () -> Unit) {}

fun main() {
    submit1 { "" }.length
    submit2 { "" }.length
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, lambdaLiteral, stringLiteral */

// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis
// ISSUE: KT-81444

fun a(b: () -> Unit = {}, c: () -> Int) {}
@JvmName("a2")
fun a(b: () -> Unit = {}, c: () -> String) {}

fun myUnit() {}

fun c() {
    a { "" } 
    a(b = { myUnit() }) { "" }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, lambdaLiteral, stringLiteral */

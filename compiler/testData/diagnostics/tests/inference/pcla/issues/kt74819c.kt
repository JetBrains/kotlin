// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-74819
// WITH_STDLIB

fun foo(x: List<String>) {
    buildList {
        flatMap { x }
        add("")
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral, stringLiteral */

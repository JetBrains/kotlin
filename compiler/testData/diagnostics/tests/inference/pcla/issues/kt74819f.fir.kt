// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-74819
// WITH_STDLIB

fun foo(x: List<String>) =
    buildList {
        add("")
        addAll(<!ARGUMENT_TYPE_MISMATCH!>flatMap { listOf(2) }<!>)
        addAll(flatMap { x })
    }

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, lambdaLiteral, stringLiteral */

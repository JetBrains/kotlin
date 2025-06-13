// RUN_PIPELINE_TILL: BACKEND
// RENDER_DIAGNOSTICS_FULL_TEXT
// FIR_DUMP

fun bar() {
    buildList {
        add("Boom")
        println(<!TYPE_MISMATCH!>this<!>.plus(1)[0])
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, intersectionType, lambdaLiteral, stringLiteral,
thisExpression */

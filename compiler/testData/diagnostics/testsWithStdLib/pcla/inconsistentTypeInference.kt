// RUN_PIPELINE_TILL: BACKEND
// FIR_DUMP

fun foo() {
    buildList {
        add("Boom")
        println(plus(1)[0])
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, intersectionType, lambdaLiteral, stringLiteral */

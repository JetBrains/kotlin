// RUN_PIPELINE_TILL: BACKEND
// FIR_DUMP

fun bar() {
    buildList {
        add("Boom")
        println(this.plus(1)[0])
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, intersectionType, lambdaLiteral, stringLiteral,
thisExpression */

// RUN_PIPELINE_TILL: BACKEND
val zero = 0

fun test() {
    -0
    -0L
    -0.0
    -(1 - 1)
    -zero

    +0
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, propertyDeclaration, unaryExpression */

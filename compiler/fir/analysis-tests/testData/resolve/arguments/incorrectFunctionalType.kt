// RUN_PIPELINE_TILL: BACKEND
fun foo(func: Int.(Int) -> Int) {}

fun test() {
    foo {
        this + it
    }
}

/* GENERATED_FIR_TAGS: additiveExpression, functionDeclaration, functionalType, lambdaLiteral, thisExpression,
typeWithExtension */

// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-15919

// KT-15919: Wrong label resolution for return statement inside lambda
fun x(body: () -> Unit) {}

fun x() {
    x {
        return@x // should be resolved to the lambda's enclosing call x { ... }
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, lambdaLiteral */

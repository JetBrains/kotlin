// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters +ExplicitContextArguments

context(s: String)
fun foo(f: () -> Unit) {}

fun test() {
    foo(s = "") {}
    with("") {
        foo {}
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionDeclarationWithContext, functionalType, lambdaLiteral, stringLiteral */

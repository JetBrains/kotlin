// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

fun foo() {
    <!NO_CONTEXT_ARGUMENT("_: String"), NO_CONTEXT_ARGUMENT("_: Int")!>bar<!>()
}

context(_: String, _: Int)
fun bar() {}

/* GENERATED_FIR_TAGS: functionDeclaration, functionDeclarationWithContext */

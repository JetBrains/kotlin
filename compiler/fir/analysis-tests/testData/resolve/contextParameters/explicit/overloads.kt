// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters +ExplicitContextArguments

context(_: Boolean, a: String)
fun foo() = 1

context(_: Boolean)
fun foo(a: CharSequence) = true

fun test() {
    with(true) {
        foo(a = "")
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionDeclarationWithContext, integerLiteral, lambdaLiteral, stringLiteral */

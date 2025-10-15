// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters +ExplicitContextArguments

context(_: Boolean, a: String)
fun foo() = 1

context(_: Boolean)
fun foo(a: CharSequence) = true

fun test() {
    <!CANNOT_INFER_PARAMETER_TYPE!>with<!>(true) {
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(a = "")
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionDeclarationWithContext, integerLiteral, lambdaLiteral, stringLiteral */

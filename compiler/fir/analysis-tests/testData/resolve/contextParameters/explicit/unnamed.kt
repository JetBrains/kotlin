// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters +ExplicitContextArguments

context(_: String)
fun one() {}

context(_: String, _: Int)
fun two() {}

fun test() {
    <!NO_CONTEXT_ARGUMENT!>one<!>(<!NAMED_PARAMETER_NOT_FOUND!>_<!> = "")
    <!NO_CONTEXT_ARGUMENT, NO_CONTEXT_ARGUMENT!>two<!>(<!NAMED_PARAMETER_NOT_FOUND!>_<!> = "", <!NAMED_PARAMETER_NOT_FOUND!>_<!> = 1)

    with("") {
        <!NO_CONTEXT_ARGUMENT!>two<!>(<!NAMED_PARAMETER_NOT_FOUND!>_<!> = 1)
    }

    with(1) {
        <!NO_CONTEXT_ARGUMENT!>two<!>(<!NAMED_PARAMETER_NOT_FOUND!>_<!> = "")
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionDeclarationWithContext, integerLiteral, lambdaLiteral, stringLiteral */

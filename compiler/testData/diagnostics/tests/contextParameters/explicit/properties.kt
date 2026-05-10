// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters +ExplicitContextArguments

context(a: Int)
val prop0: String
    get() = ""

context(a: Int)
val prop1: () -> String
    get() = { "" }

fun test() {
    <!NO_CONTEXT_ARGUMENT!>prop0<!>(a = 42)
    <!NO_CONTEXT_ARGUMENT!>prop1<!>(a = 42)
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, functionDeclarationWithContext, integerLiteral,
operator, stringLiteral */

// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextReceivers, -ContextParameters
// ISSUE: KT-76527

fun foo(f: <!CONTEXT_RECEIVERS_DEPRECATED!>context<!>(String) Int.() -> Unit) {
    f("", 1)
    with("") {
        f(1)
        1.f()
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, lambdaLiteral, stringLiteral,
typeWithContext, typeWithExtension */

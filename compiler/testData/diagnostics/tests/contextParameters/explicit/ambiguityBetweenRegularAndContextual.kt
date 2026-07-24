// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
// LANGUAGE_FEATURE_TOGGLED: ExplicitContextArguments
// MODULE: m1
context(x: Int)
fun foo(a: String) = "CONTEXT"

fun foo(a: String, x: Int) = "NO-CONTEXT"

// MODULE: m2(m1)
context(x: Int)
fun bar(a: String) = "CONTEXT"

fun bar(a: String, x: Int) = "NO-CONTEXT"

fun test() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(a = "OK", x = 5)
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>bar<!>(a = "OK", x = 5)
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionDeclarationWithContext, integerLiteral, stringLiteral */

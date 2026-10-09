// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

fun foo(block: (Int) -> Unit) = Unit
fun foo(block: () -> Unit) = Unit

fun test() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>({ <!UNRESOLVED_REFERENCE!>UNRESOLVED<!> })
    foo(block = { <!UNRESOLVED_REFERENCE!>UNRESOLVED<!> })
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, lambdaLiteral, stringLiteral */

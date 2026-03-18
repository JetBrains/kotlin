// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-8686

// KT-8686: Error reporting from call resolver - outer range should not be highlighted when inner has errors
fun foo(x: Int, y: Int) {}

fun test() {
    foo(1, <!UNRESOLVED_REFERENCE!>unresolvedName<!>)
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral */
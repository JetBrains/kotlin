// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

fun foo(g: () -> Int) {}
fun foo(f: (Int) -> Int) {}

fun test() {
    foo { -> 42 }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, integerLiteral, lambdaLiteral */

// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// SKIP_TXT
// DIAGNOSTICS: -UNUSED_PARAMETER

fun foo(f: () -> Unit) {}
fun bar(): Int = 42
fun test() {
    foo {
        ::bar // should be fine
    }
    foo {
        { "something" } // should be fine
    }
}

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, functionalType, integerLiteral, lambdaLiteral,
stringLiteral */

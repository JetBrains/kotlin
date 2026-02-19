// RUN_PIPELINE_TILL: FRONTEND

fun foo(f: suspend () -> Unit) {
}

fun bar() {
    foo(<!TYPE_MISMATCH!>fun () {}<!>)
}

/* GENERATED_FIR_TAGS: anonymousFunction, functionDeclaration, functionalType, suspend */

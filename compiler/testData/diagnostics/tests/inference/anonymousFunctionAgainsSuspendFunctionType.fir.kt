// RUN_PIPELINE_TILL: FRONTEND

fun foo(f: suspend () -> Unit) {
}

fun bar() {
    foo(<!ARGUMENT_TYPE_MISMATCH("SuspendFunction0<Unit>; Function0<Unit>")!>fun () {}<!>)
}

/* GENERATED_FIR_TAGS: anonymousFunction, functionDeclaration, functionalType, suspend */

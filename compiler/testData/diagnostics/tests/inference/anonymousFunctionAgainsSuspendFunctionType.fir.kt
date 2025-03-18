// RUN_PIPELINE_TILL: FRONTEND

fun foo(f: suspend () -> Unit) {
}

fun bar() {
    foo(<!ARGUMENT_TYPE_MISMATCH("SuspendFunction0<Unit>; SuspendFunction0<Unit>")!>fun () {}<!>)
}
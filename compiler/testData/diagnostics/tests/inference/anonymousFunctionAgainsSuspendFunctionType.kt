// RUN_PIPELINE_TILL: FRONTEND

fun foo(f: suspend () -> Unit) {
}

fun bar() {
    foo(<!TYPE_MISMATCH!>fun () {}<!>)
}
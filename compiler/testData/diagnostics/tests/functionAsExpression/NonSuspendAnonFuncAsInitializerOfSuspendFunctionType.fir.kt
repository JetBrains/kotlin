// RUN_PIPELINE_TILL: FRONTEND
fun test () {
    val local: suspend () -> Unit = <!INITIALIZER_TYPE_MISMATCH("SuspendFunction0<Unit>; Function0<Unit>")!>fun () {}<!>;
}

// RUN_PIPELINE_TILL: FRONTEND
fun test () {
    val local: suspend () -> Unit = <!TYPE_MISMATCH("suspend () -> Unit; () -> Unit")!>fun () {}<!>;
}

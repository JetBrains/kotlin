// RUN_PIPELINE_TILL: FRONTEND
fun test () {
    val local: suspend () -> Unit = <!ARGUMENT_TYPE_MISMATCH!>fun () {}<!>;
}

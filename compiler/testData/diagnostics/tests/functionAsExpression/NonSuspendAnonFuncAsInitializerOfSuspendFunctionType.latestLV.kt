// RUN_PIPELINE_TILL: FRONTEND
// LATEST_LV_DIFFERENCE
fun test () {
    val local: suspend () -> Unit = <!ARGUMENT_TYPE_MISMATCH!>fun () {}<!>;
}

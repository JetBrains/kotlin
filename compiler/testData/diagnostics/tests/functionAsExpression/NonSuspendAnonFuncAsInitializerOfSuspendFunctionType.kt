fun test () {
    val local: suspend () -> Unit = <!TYPE_MISMATCH("suspend () -> Unit; () -> Unit")!>fun () {}<!>;
}

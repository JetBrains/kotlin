fun test () {
    val local: suspend () -> Unit = <!INITIALIZER_TYPE_MISMATCH("kotlin.coroutines.SuspendFunction0<kotlin.Unit>; kotlin.Function0<kotlin.Unit>")!>fun () {}<!>;
}

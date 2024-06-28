fun produceConcreteA(func: () -> (suspend () -> Unit)): suspend () -> Unit = func()
fun (() -> (suspend () -> Unit)).produceConcreteB(): suspend () -> Unit = this()

fun test() {
    fun produce(): suspend () -> Unit = <!RETURN_TYPE_MISMATCH("kotlin.coroutines.SuspendFunction0<kotlin.Unit>; kotlin.Function0<kotlin.Unit>")!>fun () {}<!>;

    produceConcreteA { <!ARGUMENT_TYPE_MISMATCH("kotlin.coroutines.SuspendFunction0<kotlin.Unit>; kotlin.Function0<kotlin.Unit>")!>fun () {}<!> };
    produceConcreteA(fun () = <!ARGUMENT_TYPE_MISMATCH("kotlin.coroutines.SuspendFunction0<kotlin.Unit>; kotlin.Function0<kotlin.Unit>")!>fun () {}<!>);

    { fun () {} }.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>produceConcreteB<!>();
    (fun () = fun () {}).<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>produceConcreteB<!>();
}

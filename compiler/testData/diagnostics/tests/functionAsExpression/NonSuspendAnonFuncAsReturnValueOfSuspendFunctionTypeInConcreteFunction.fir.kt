// RUN_PIPELINE_TILL: FRONTEND
fun produceConcreteA(func: () -> (suspend () -> Unit)): suspend () -> Unit = func()
fun (() -> (suspend () -> Unit)).produceConcreteB(): suspend () -> Unit = this()

fun test() {
    fun produce(): suspend () -> Unit = <!RETURN_TYPE_MISMATCH("SuspendFunction0<Unit>; Function0<Unit>")!>fun () {}<!>;

    produceConcreteA { fun () {} };
    produceConcreteA(fun () = fun () {});

    { fun () {} }.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>produceConcreteB<!>();
    (fun () = fun () {}).<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>produceConcreteB<!>();
}

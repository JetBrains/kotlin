// RUN_PIPELINE_TILL: FRONTEND
fun produceConcreteA(func: () -> (suspend () -> Unit)): suspend () -> Unit = func()
fun (() -> (suspend () -> Unit)).produceConcreteB(): suspend () -> Unit = this()

fun test() {
    fun produce(): suspend () -> Unit = <!TYPE_MISMATCH("suspend () -> Unit; () -> Unit")!>fun () {}<!>;

    produceConcreteA { <!TYPE_MISMATCH("suspend () -> Unit; () -> Unit")!>fun () {}<!> };
    produceConcreteA(<!TYPE_MISMATCH("() -> suspend () -> Unit; () -> () -> Unit")!>fun () = <!TYPE_MISMATCH("suspend () -> Unit; () -> Unit")!>fun () {}<!><!>);

    { fun () {} }.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>produceConcreteB<!>();
    (fun () = fun () {}).<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>produceConcreteB<!>();
}

/* GENERATED_FIR_TAGS: anonymousFunction, funWithExtensionReceiver, functionDeclaration, functionalType, lambdaLiteral,
localFunction, suspend, thisExpression */

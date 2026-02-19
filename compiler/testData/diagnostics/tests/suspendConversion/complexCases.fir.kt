// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-74932

fun produceSuspend(func: () -> (suspend () -> Unit)) {}
fun runSuspend(func: suspend () -> Unit) {}

fun <T> T.takeMe(): T = this

fun test() {
    runSuspend { }
    produceSuspend { {} } 

    val x: () -> Unit = {}
    runSuspend(x) 
    runSuspend(x.takeMe()) 

    val y: suspend () -> Unit <!INITIALIZER_TYPE_MISMATCH!>=<!> x

    produceSuspend { <!RETURN_TYPE_MISMATCH!>x<!> }
    produceSuspend { <!RETURN_TYPE_MISMATCH!>x.takeMe()<!> }

    runSuspend(<!ARGUMENT_TYPE_MISMATCH!>fun() {}<!>)
    runSuspend((fun() {}).takeMe()) 
    produceSuspend { <!RETURN_TYPE_MISMATCH!>fun() {}<!> }
    produceSuspend(fun () = <!RETURN_TYPE_MISMATCH!>fun () {}<!>)
    produceSuspend { <!RETURN_TYPE_MISMATCH!>(fun() {}).takeMe()<!> }
}

/* GENERATED_FIR_TAGS: anonymousFunction, funWithExtensionReceiver, functionDeclaration, functionalType, lambdaLiteral,
localProperty, nullableType, propertyDeclaration, suspend, thisExpression, typeParameter */

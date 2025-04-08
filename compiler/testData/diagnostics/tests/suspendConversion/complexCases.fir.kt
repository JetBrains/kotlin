// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-74932
// LATEST_LV_DIFFERENCE

fun produceSuspend(func: () -> (suspend () -> Unit)) {}
fun runSuspend(func: suspend () -> Unit) {}

fun <T> T.takeMe(): T = this

fun test() {
    runSuspend { }
    produceSuspend { {} } 

    val x: () -> Unit = {}
    runSuspend(x) 
    runSuspend(x.takeMe()) 

    val y: suspend () -> Unit = <!INITIALIZER_TYPE_MISMATCH!>x<!>

    produceSuspend { x }
    produceSuspend { <!TYPE_MISMATCH!>x.takeMe()<!> }

    runSuspend(<!ARGUMENT_TYPE_MISMATCH!>fun() {}<!>)
    runSuspend((fun() {}).takeMe()) 
    produceSuspend { fun() {} }
    produceSuspend(fun () = fun () {})
    produceSuspend { <!TYPE_MISMATCH!>(fun() {}).takeMe()<!> }
}
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

    val y: suspend () -> Unit = <!TYPE_MISMATCH!>x<!>

    produceSuspend { <!TYPE_MISMATCH, TYPE_MISMATCH!>x<!> }
    produceSuspend { <!TYPE_MISMATCH, TYPE_MISMATCH!>x.<!TYPE_MISMATCH!>takeMe()<!><!> }

    runSuspend(<!TYPE_MISMATCH!>fun() {}<!>)
    runSuspend((fun() {}).takeMe()) 
    produceSuspend { <!TYPE_MISMATCH!>fun() {}<!> }
    produceSuspend(<!TYPE_MISMATCH!>fun () = <!TYPE_MISMATCH!>fun () {}<!><!>)
    produceSuspend { <!TYPE_MISMATCH, TYPE_MISMATCH!>(fun() {}).<!TYPE_MISMATCH!>takeMe()<!><!> }
}
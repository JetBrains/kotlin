// ISSUE: KT-89451
// DUMP_IR
class C {
    fun run() {}
}

private fun isNullSuspend(func: (suspend () -> Unit)?): Boolean = func == null

private fun isNull(runnable: C): Boolean {
    val a = runnable::run
    return isNullSuspend(a)
}


fun box(): String {
    if (isNull(C())) return "fail"
    return "OK"
}

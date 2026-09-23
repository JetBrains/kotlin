// ISSUE: KT-89451
// IGNORE_BACKEND: ANY
interface I {
    fun run()
}

private fun isNullSuspend(func: (suspend () -> Unit)?): Boolean = func == null

private fun isNull(runnable: I?): Boolean =
    isNullSuspend(if (runnable == null) null else runnable::run)

fun box(): String {
    if (!isNull(null)) return "fail: null argument arrived non-null"
    return "OK"
}

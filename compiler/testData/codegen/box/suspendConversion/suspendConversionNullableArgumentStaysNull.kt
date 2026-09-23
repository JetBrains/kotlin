// ISSUE: KT-89451
// DUMP_IR
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_FIRST_STAGE: ANY:2.0,2.1,2.2,2.3,2.4
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

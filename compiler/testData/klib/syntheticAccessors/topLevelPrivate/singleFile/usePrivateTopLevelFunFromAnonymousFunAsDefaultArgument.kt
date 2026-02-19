// NO_CHECK_LAMBDA_INLINING
private fun funOK() = "OK"

@Suppress("NOT_YET_SUPPORTED_IN_INLINE")
internal inline fun internalInlineFun(ok: () -> String = fun() = funOK()) = ok()

fun box(): String {
    return internalInlineFun()
}

// IGNORE_BACKEND_K1: ANY
// NO_CHECK_LAMBDA_INLINING
private fun funOK() = "OK"

internal inline fun internalInlineFun(ok: () -> String = fun() = funOK()) = ok()

fun box(): String {
    return internalInlineFun()
}

// NO_CHECK_LAMBDA_INLINING
private fun funOK() = "OK"

private fun privateFun(x: () -> String = fun() = funOK(), y: () -> String = x) = y()

internal inline fun internalFun() = privateFun()

fun box() : String {
    return internalFun()
}

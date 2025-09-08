// NO_CHECK_LAMBDA_INLINING
// FILE: A.kt
private fun funOK() = "OK"

private fun privateFun(x: () -> String = fun() = funOK(), y: () -> String = x) = y()

internal inline fun internalFun() = privateFun()

// FILE: B.kt
fun box() : String {
    return internalFun()
}

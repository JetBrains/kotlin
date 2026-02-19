// NO_CHECK_LAMBDA_INLINING
private fun privateFunO(x: () -> String = { "O" }, y: () -> String = x) = y()

internal inline fun internalFunO() = privateFunO()

private fun funK() = { "K" }

private fun privateFunK(x: () -> String = funK(), y: () -> String = x) = y()

internal inline fun internalFunK() = privateFunK()

fun box() : String {
    return internalFunO() + internalFunK()
}

// NO_CHECK_LAMBDA_INLINING
// MODULE: lib
// FILE: A.kt
private fun privateFunO(x: () -> String = { "O" }, y: () -> String = x) = y()

internal inline fun internalFunO() = privateFunO()

private fun funK() = { "K" }

private fun privateFunK(x: () -> String = funK(), y: () -> String = x) = y()

internal inline fun internalFunK() = privateFunK()

// MODULE: main()(lib)
// FILE: B.kt
fun box() : String {
    return internalFunO() + internalFunK()
}

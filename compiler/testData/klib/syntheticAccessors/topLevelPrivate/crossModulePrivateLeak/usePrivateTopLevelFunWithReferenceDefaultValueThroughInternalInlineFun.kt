// NO_CHECK_LAMBDA_INLINING
// MODULE: lib
// FILE: A.kt
private fun funOK() = "OK"

private fun privateFun(x: () -> String = ::funOK, y: () -> String = x) = y()

internal inline fun internalFun() = privateFun()

// MODULE: main()(lib)
// FILE: B.kt
fun box() : String {
    return internalFun()
}

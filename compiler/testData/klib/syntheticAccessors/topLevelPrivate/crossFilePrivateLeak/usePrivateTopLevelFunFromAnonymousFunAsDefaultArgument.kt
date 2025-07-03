// TARGET_BACKEND: NATIVE, JS_IR, WASM
//NO_CHECK_LAMBDA_INLINING
// FILE: a.kt
private fun funOK() = "OK"

internal inline fun internalInlineFun(ok: () -> String = fun() = funOK()) = ok()

// FILE: main.kt
fun box(): String {
    return internalInlineFun()
}

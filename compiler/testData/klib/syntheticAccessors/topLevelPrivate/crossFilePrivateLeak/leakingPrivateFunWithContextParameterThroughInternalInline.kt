// IGNORE_KLIB_SYNTHETIC_ACCESSORS_CHECKS: JS_IR, WASM
// LANGUAGE: +ContextParameters
// FILE: A.kt
class Scope {
    val ok = "OK"
}

context(scope: Scope)
private fun privateFun() = scope.ok

internal inline fun internalInlineFun() = with(Scope()) {
    privateFun()
}

// FILE: B.kt
fun box(): String = internalInlineFun()

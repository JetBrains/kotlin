// IGNORE_KLIB_SYNTHETIC_ACCESSORS_CHECKS: JS_IR
// LANGUAGE: +ContextParameters
class Scope {
    val ok = "OK"
}

context(scope: Scope)
private fun privateFun() = scope.ok

internal inline fun internalInlineFun() = with(Scope()) {
    privateFun()
}

fun box(): String = internalInlineFun()

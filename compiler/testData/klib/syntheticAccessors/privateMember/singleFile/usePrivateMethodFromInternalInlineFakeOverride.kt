// ISSUE: KT-78960
// IGNORE_BACKEND: WASM
// ^^^ KT-78960: java.lang.NullPointerException: null
//                 at org.jetbrains.kotlin.backend.wasm.lower.EraseVirtualDispatchReceiverParametersTypes.lower

open class A {
    private fun privateMethod() = "OK"

    internal inline fun internalInlineMethod() = privateMethod()
}

class B: A()

fun box(): String {
    return B().internalInlineMethod()
}

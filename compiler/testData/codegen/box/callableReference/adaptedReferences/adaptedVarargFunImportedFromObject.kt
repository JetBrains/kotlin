// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: BINDING_RECEIVERS
// IGNORE_BACKEND_FIR: JVM_IR

import Host.foo

fun withO(fn: (String) -> String) = fn("O")

object Host {
    fun foo(vararg x: String) = x[0] + "K"
}

fun box() = withO(::foo)

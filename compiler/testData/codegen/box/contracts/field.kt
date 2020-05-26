// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// IGNORE_BACKEND: NATIVE
// WITH_RUNTIME
// IGNORE_LIGHT_ANALYSIS

import kotlin.contracts.*

fun runOnce(action: () -> Unit) {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }
    action()
}

class Foo {
    val res: String
    init {
        runOnce {
            res = "OK"
        }
    }
}

fun box(): String {
    return Foo().res
}

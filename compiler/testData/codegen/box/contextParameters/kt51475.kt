// LANGUAGE: +ContextParameters
// IGNORE_BACKEND_K1: ANY
// WITH_STDLIB
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

class Smth {
    val whatever: Int

    init {
        calculate { whatever = it }
    }

    context(any: Any)
    inline fun calculate(block: (Int) -> Unit) {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
    }
}

fun box(): String {
    val s = Smth()
    return "OK"
}
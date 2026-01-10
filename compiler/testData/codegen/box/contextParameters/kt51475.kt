// LANGUAGE: +ContextParameters
// IGNORE_BACKEND_K1: ANY
// WITH_STDLIB
// OPT_IN: kotlin.contracts.ExperimentalContracts

// FILE: lib.kt
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

// FILE: main.kt
fun box(): String {
    val s = Smth()
    return "OK"
}
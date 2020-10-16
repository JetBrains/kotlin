// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// IGNORE_BACKEND: NATIVE
// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME

import kotlin.contracts.*

class Smth {
    val whatever: Int

    init {
        calculate({ whatever = it })
    }

    @OptIn(ExperimentalContracts::class)
    private inline fun calculate(block: (Int) -> Unit) {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        block(42)
    }
}

fun box(): String {
    val smth = Smth()
    return if (smth.whatever == 42) "OK" else "FAIL ${smth.whatever}"
}
// !OPT_IN: kotlin.contracts.ExperimentalContracts
// WITH_STDLIB

// JVM_ABI_K1_K2_DIFF: KT-62464

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
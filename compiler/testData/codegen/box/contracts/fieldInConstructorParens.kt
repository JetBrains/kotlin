// OPT_IN: kotlin.contracts.ExperimentalContracts
// WITH_STDLIB

// FILE: lib.kt
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

// FILE: main.kt
fun box(): String {
    val smth = Smth()
    return if (smth.whatever == 42) "OK" else "FAIL ${smth.whatever}"
}

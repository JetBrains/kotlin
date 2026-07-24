import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind

fun main() {
    var local: String? = ""

    callInPlace {
        local = null
    }

    callInPlace {
        if (local != null) {
            println(<expr>local</expr>.length)
        }
    }
}

@OptIn(ExperimentalContracts::class)
fun callInPlace(block: () -> Unit) {
    kotlin.contracts.contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
}

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind

fun main() {
    var local: String? = ""
    if (local != null) {
        callInPlace {
            local = "value"
        }
        println(<expr>local</expr>.length)
    }
}

@OptIn(ExperimentalContracts::class)
fun callInPlace(block: () -> Unit) {
    kotlin.contracts.contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
}

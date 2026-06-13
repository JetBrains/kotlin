import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind

fun main() {
    var local: C? = null
    local = C()
    callInPlace {
        println(<expr>local</expr> != null)
    }
}

@OptIn(ExperimentalContracts::class)
fun callInPlace(block: () -> Unit) {
    kotlin.contracts.contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
}

class C

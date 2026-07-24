import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind

fun main() {
    var local: C? = C()
    callInPlace {
        local = C()
    }
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

class C {
    val c: C? = null
}

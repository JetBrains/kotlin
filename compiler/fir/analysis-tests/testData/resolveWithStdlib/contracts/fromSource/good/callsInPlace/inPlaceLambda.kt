// !DUMP_CFG
import kotlin.contracts.*

@ExperimentalContracts
fun bar(x: () -> Unit) {
    contract {
        callsInPlace(x, InvocationKind.AT_MOST_ONCE)
    }

    if (true) {
        x()
    }
}

@ExperimentalContracts
fun foo(x: () -> Unit) {
    contract {
        callsInPlace(x, InvocationKind.AT_LEAST_ONCE)
    }

    x()

    bar {
        x()
    }
}

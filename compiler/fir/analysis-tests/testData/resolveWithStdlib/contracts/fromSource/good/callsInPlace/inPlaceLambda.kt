// !DUMP_CFG
import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
fun bar(x: () -> Unit) {
    contract {
        callsInPlace(x, InvocationKind.AT_MOST_ONCE)
    }

    if (true) {
        x()
    }
}

@OptIn(ExperimentalContracts::class)
fun foo(x: () -> Unit) {
    contract {
        callsInPlace(x, InvocationKind.AT_LEAST_ONCE)
    }

    x()

    bar {
        x()
    }
}

// !DUMP_CFG
import kotlin.contracts.*

@ExperimentalContracts
fun bar(x: () -> Unit) {
    contract {
        callsInPlace(x, InvocationKind.EXACTLY_ONCE)
    }

    if (true) {
        x.invoke()
        return
    }

    bar(x)
}

@ExperimentalContracts
fun foo(x: () -> Unit, y: () -> Unit, z: () -> Unit) {
    contract {
        callsInPlace(x, InvocationKind.UNKNOWN)
        callsInPlace(y, InvocationKind.EXACTLY_ONCE)
        callsInPlace(z, InvocationKind.AT_LEAST_ONCE)
    }

    if (true) {
        for (i in 0..0) {
            x.invoke()
        }

        y.invoke()
    } else {
        if (false) {
            y.invoke()
        } else {
            y.invoke()
            z.invoke()
            return
        }
    }

    do {
        bar(z)
    } while (true)
}
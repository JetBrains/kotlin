// !DUMP_CFG
import kotlin.contracts.*

@ExperimentalContracts
fun bar(x: () -> Unit) {
    contract {
        callsInPlace(x, InvocationKind.EXACTLY_ONCE)
    }

    x.invoke()
}

@ExperimentalContracts
fun (() -> Unit).baz() {
    contract {
        callsInPlace(this@baz, InvocationKind.AT_MOST_ONCE)
    }

    if(true){
        this.invoke()
    }
}

@ExperimentalContracts
fun foo(x: () -> Unit, y: () -> Unit) {
    contract {
        callsInPlace(x, InvocationKind.AT_LEAST_ONCE)
        callsInPlace(y, InvocationKind.AT_MOST_ONCE)
    }

    if (true) {
        x.invoke()
        y.baz()
        return
    }

    bar(x)
}
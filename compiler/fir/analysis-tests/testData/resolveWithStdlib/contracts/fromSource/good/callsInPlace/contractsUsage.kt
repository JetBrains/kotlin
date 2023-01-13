// !DUMP_CFG
import kotlin.contracts.*

@OptIn(ExperimentalContracts::class)
fun bar(x: () -> Unit) {
    contract {
        callsInPlace(x, InvocationKind.EXACTLY_ONCE)
    }

    x.invoke()
}

@OptIn(ExperimentalContracts::class)
fun (() -> Unit).baz() {
    contract {
        callsInPlace(this@baz, InvocationKind.AT_MOST_ONCE)
    }

    if(true){
        this.invoke()
    }
}

@OptIn(ExperimentalContracts::class)
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
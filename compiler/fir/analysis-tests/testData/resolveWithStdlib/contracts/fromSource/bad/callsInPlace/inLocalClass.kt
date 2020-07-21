// !DUMP_CFG
import kotlin.contracts.*

@ExperimentalContracts
fun foo(a: () -> Unit, b: () -> Unit, c: () -> Unit, d: () -> Unit, e: () -> Unit) {
    <!CAPTURED_IN_PLACE_LAMBDA, CAPTURED_IN_PLACE_LAMBDA, CAPTURED_IN_PLACE_LAMBDA, CAPTURED_IN_PLACE_LAMBDA!>contract {
        callsInPlace(a, InvocationKind.AT_MOST_ONCE)
        callsInPlace(b, InvocationKind.AT_MOST_ONCE)
        callsInPlace(c, InvocationKind.AT_MOST_ONCE)
        callsInPlace(d, InvocationKind.AT_MOST_ONCE)
        callsInPlace(e, InvocationKind.AT_MOST_ONCE)
    }<!>

    class LocalClass {

        val leakedVal = a
        val leaked: Any

        constructor() {
            b()
        }

        init {
            leaked = c
        }

        fun run() {
            d()
        }
    }

    LocalClass().run()

    e()
}

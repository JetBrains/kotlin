// !DUMP_CFG
import kotlin.contracts.*

@ExperimentalContracts
fun foo(a: () -> Unit, b: () -> Unit, c: () -> Unit, d: () -> Unit) {
    <!CAPTURED_IN_PLACE_LAMBDA, CAPTURED_IN_PLACE_LAMBDA, CAPTURED_IN_PLACE_LAMBDA!>contract {
        callsInPlace(a, InvocationKind.AT_MOST_ONCE)
        callsInPlace(b, InvocationKind.AT_MOST_ONCE)
        callsInPlace(c, InvocationKind.AT_MOST_ONCE)
        callsInPlace(d, InvocationKind.AT_MOST_ONCE)
    }<!>

    val obj = object : Runnable {

        val leakedVal = a
        val leaked: Any

        init {
            leaked = b
        }

        override fun run() {
            c()
        }

    }

    obj.run()

    d()
}
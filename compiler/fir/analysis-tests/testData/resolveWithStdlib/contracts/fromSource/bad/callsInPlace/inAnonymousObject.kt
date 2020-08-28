// !DUMP_CFG
import kotlin.contracts.*

@ExperimentalContracts
fun foo(a: () -> Unit, b: () -> Unit, c: () -> Unit, d: () -> Unit) {
    <!LEAKED_IN_PLACE_LAMBDA, LEAKED_IN_PLACE_LAMBDA, LEAKED_IN_PLACE_LAMBDA!>contract {
        callsInPlace(a, InvocationKind.AT_MOST_ONCE)
        callsInPlace(b, InvocationKind.AT_MOST_ONCE)
        callsInPlace(c, InvocationKind.AT_MOST_ONCE)
        callsInPlace(d, InvocationKind.AT_MOST_ONCE)
    }<!>

    val obj = object : Runnable {

        val leakedVal = <!LEAKED_IN_PLACE_LAMBDA!>a<!>
        val leaked: Any

        init {
            leaked = <!LEAKED_IN_PLACE_LAMBDA!>b<!>
        }

        override fun run() {
            <!LEAKED_IN_PLACE_LAMBDA!>c()<!>
        }

    }

    obj.run()

    d()
}
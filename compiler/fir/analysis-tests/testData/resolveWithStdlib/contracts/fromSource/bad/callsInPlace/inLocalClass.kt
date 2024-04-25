// DUMP_CFG
import kotlin.contracts.*

@ExperimentalContracts
fun foo(a: () -> Unit, b: () -> Unit, c: () -> Unit, d: () -> Unit, e: () -> Unit) {
    contract {
        <!LEAKED_IN_PLACE_LAMBDA!>callsInPlace(a, InvocationKind.AT_MOST_ONCE)<!>
        <!LEAKED_IN_PLACE_LAMBDA!>callsInPlace(b, InvocationKind.AT_MOST_ONCE)<!>
        <!LEAKED_IN_PLACE_LAMBDA!>callsInPlace(c, InvocationKind.AT_MOST_ONCE)<!>
        <!LEAKED_IN_PLACE_LAMBDA!>callsInPlace(d, InvocationKind.AT_MOST_ONCE)<!>
        callsInPlace(e, InvocationKind.AT_MOST_ONCE)
    }

    class LocalClass {

        val leakedVal = <!LEAKED_IN_PLACE_LAMBDA!>a<!>
        val leaked: Any

        constructor() {
            <!LEAKED_IN_PLACE_LAMBDA!>b<!>()
        }

        init {
            leaked = <!LEAKED_IN_PLACE_LAMBDA!>c<!>
        }

        fun run() {
            <!LEAKED_IN_PLACE_LAMBDA!>d<!>()
        }
    }

    LocalClass().run()

    e()
}

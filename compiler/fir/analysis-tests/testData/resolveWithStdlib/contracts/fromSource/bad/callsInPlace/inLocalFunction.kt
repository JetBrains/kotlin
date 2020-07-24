// !DUMP_CFG
import kotlin.contracts.*

@ExperimentalContracts
fun foo(a: () -> Unit, b: () -> Unit) {
    <!CAPTURED_IN_PLACE_LAMBDA!>contract {
        callsInPlace(a, InvocationKind.AT_MOST_ONCE)
    }<!>

    fun localFun() {
        a()
    }

    localFun()

    b()
}

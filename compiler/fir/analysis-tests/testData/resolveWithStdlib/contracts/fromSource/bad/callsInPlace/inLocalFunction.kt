// !DUMP_CFG
import kotlin.contracts.*

@ExperimentalContracts
fun foo(a: () -> Unit, b: () -> Unit) {
    <!LEAKED_IN_PLACE_LAMBDA!>contract {
        callsInPlace(a, InvocationKind.AT_MOST_ONCE)
    }<!>

    fun localFun() {
        <!LEAKED_IN_PLACE_LAMBDA!>a<!>.invoke()
        <!LEAKED_IN_PLACE_LAMBDA!>a()<!>
    }

    localFun()

    b()
}

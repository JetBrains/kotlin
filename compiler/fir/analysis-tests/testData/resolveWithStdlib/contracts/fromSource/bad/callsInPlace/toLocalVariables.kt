// !DUMP_CFG
import kotlin.contracts.*

fun bar(x: () -> Unit) {

}

@ExperimentalContracts
fun foo(x: () -> Unit, y: () -> Unit, z: () -> Unit) {
    <!LEAKED_IN_PLACE_LAMBDA, LEAKED_IN_PLACE_LAMBDA, LEAKED_IN_PLACE_LAMBDA!>contract {
        callsInPlace(x, InvocationKind.AT_MOST_ONCE)
        callsInPlace(y, InvocationKind.AT_MOST_ONCE)
        callsInPlace(z, InvocationKind.AT_MOST_ONCE)
    }<!>

    if (true) {
        bar(<!LEAKED_IN_PLACE_LAMBDA!>x<!>)
    } else {
        val yCopy = <!LEAKED_IN_PLACE_LAMBDA!>y<!>
        yCopy()
    }

    val zCopy: () -> Unit
    zCopy = <!LEAKED_IN_PLACE_LAMBDA!>z<!>
    zCopy()
}
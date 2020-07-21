// !DUMP_CFG
import kotlin.contracts.*

fun bar(x: () -> Unit) {

}

@ExperimentalContracts
fun foo(x: () -> Unit, y: () -> Unit, z: () -> Unit) {
    <!CAPTURED_IN_PLACE_LAMBDA, CAPTURED_IN_PLACE_LAMBDA, CAPTURED_IN_PLACE_LAMBDA!>contract {
        callsInPlace(x, InvocationKind.AT_MOST_ONCE)
        callsInPlace(y, InvocationKind.AT_MOST_ONCE)
        callsInPlace(z, InvocationKind.AT_MOST_ONCE)
    }<!>

    if (true) {
        bar(x)
    } else {
        val yCopy = y
        yCopy()
    }

    val zCopy: () -> Unit
    zCopy = z
    zCopy()
}
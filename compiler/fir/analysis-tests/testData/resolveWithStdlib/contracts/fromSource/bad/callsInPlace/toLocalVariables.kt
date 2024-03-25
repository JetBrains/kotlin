// !DUMP_CFG
import kotlin.contracts.*

fun bar(x: () -> Unit) {

}

@ExperimentalContracts
fun foo(x: () -> Unit, y: () -> Unit, z: () -> Unit) {
    contract {
        <!LEAKED_IN_PLACE_LAMBDA!>callsInPlace(x, InvocationKind.AT_MOST_ONCE)<!>
        <!LEAKED_IN_PLACE_LAMBDA!>callsInPlace(y, InvocationKind.AT_MOST_ONCE)<!>
        <!LEAKED_IN_PLACE_LAMBDA!>callsInPlace(z, InvocationKind.AT_MOST_ONCE)<!>
    }

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
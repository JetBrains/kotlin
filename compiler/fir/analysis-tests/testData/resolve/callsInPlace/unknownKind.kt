// !DUMP_CFG
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

fun bar(x: () -> Unit) {

}

@ExperimentalContracts
fun foo(x: () -> Unit, y: () -> Unit, z: () -> Unit) {
    contract {
        callsInPlace(x, InvocationKind.UNKNOWN)
        callsInPlace(y, InvocationKind.UNKNOWN)
        callsInPlace(z, InvocationKind.UNKNOWN)
    }

    if (true) {
        bar(x)
    } else {
        val yCopy = y
    }

    val zCopy: () -> Unit
    zCopy = z
}
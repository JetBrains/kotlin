// WITH_STDLIB
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun twoContracts(foo: Any?, bar: Any?, block: () -> Unit): Boolean {
    contr<caret>act {
        returns(true) implies (foo == null && bar != null)
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
    return foo == null && bar != null
}

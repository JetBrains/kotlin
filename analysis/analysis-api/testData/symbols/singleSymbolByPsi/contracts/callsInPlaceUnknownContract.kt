// WITH_STDLIB
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun unknownContract(block: () -> Unit) {
    contr<caret>act {
        callsInPlace(block, InvocationKind.UNKNOWN)
    }
    block()
}

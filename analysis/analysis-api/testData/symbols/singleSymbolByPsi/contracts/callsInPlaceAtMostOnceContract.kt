// WITH_STDLIB
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun atMostOnceContract(block: () -> Unit) {
    contr<caret>act {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    block()
}

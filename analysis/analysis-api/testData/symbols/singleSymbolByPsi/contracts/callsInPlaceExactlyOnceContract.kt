// WITH_STDLIB
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun exactlyOnceContract(block: () -> Unit) {
    contr<caret>act {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
}

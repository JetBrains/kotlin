// WITH_STDLIB
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun (() -> Unit).referenceReceiverInContract() {
    contr<caret>act {
        callsInPlace(this@referenceReceiverInContract, InvocationKind.EXACTLY_ONCE)
    }
    this()
}

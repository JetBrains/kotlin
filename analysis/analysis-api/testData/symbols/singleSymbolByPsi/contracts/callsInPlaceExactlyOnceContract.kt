// WITH_STDLIB
// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1
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

// WITH_STDLIB
// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1
// RETURN_VALUE_CHECKER_MODE: FULL
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
inline fun <T, R> T.myLet(block: (T) -> R): R {
    contr<caret>act {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        returnsResultOf(block)
    }
    return block(this)
}

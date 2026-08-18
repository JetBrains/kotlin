// WITH_STDLIB
// RETURN_VALUE_CHECKER_MODE: FULL
// LANGUAGE: +AllowReturnsResultOfContract

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

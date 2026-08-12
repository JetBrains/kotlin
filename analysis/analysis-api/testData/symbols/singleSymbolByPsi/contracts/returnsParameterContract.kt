// WITH_STDLIB
// DO_NOT_REQUIRE_NON_PSI_SYMBOL_RESTORATION_K1
// RETURN_VALUE_CHECKER_MODE: FULL

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
inline fun <T> T.myApply(block: T.() -> Unit): T {
    contr<caret>act {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        returnsParameter(this@myApply)
    }
    block()
    return this
}

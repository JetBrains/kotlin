// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)

 SECTION: contracts
 CATEGORIES: declarations, contractBuilder, common
 NUMBER: 2
 DESCRIPTION: Functions with contract and builder lambda in parentheses.
 */

import kotlin.contracts.*

inline fun case_1(block: () -> Unit) {
    contract({ callsInPlace(block, InvocationKind.EXACTLY_ONCE) })
    return block()
}

inline fun case_2(block: () -> Unit) {
    contract(builder = { callsInPlace(block, InvocationKind.EXACTLY_ONCE) })
    return block()
}

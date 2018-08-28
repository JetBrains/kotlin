// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)

 SECTION: contracts
 CATEGORY: declarations, contractBuilder, common
 NUMBER: 3
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

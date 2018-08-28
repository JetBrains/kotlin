// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)

 SECTION: contracts
 CATEGORY: declarations, contractBuilder, effects, callsInPlace
 NUMBER: 1
 DESCRIPTION: contract functions with CallsInPlace effects with different invocation kinds.
 */

import kotlin.contracts.*

inline fun case_1(block: () -> Unit) {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return block()
}

inline fun case_2(block: () -> Unit) {
    contract { callsInPlace(block, InvocationKind.AT_MOST_ONCE) }
    return block()
}

inline fun case_3(block: () -> Unit) {
    contract { callsInPlace(block, InvocationKind.AT_LEAST_ONCE) }
    return block()
}

inline fun case_4(block: () -> Unit) {
    contract { callsInPlace(block, InvocationKind.UNKNOWN) }
    return block()
}

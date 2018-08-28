// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)

 SECTION: contracts
 CATEGORY: declarations, contractBuilder, effects, callsInPlace
 NUMBER: 1
 DESCRIPTION: contract functions with CallsInPlace with dynamic InvocationKind.
 ISSUES: KT-26152
 */

import kotlin.contracts.*

internal val invocationKind: InvocationKind = InvocationKind.EXACTLY_ONCE

internal object SampleObject {
    val invocationKind = InvocationKind.EXACTLY_ONCE
}

internal inline fun case_1(invocationKind: InvocationKind, block: () -> Unit) {
    contract { callsInPlace(block, <!ERROR_IN_CONTRACT_DESCRIPTION!>invocationKind<!>) }
    return block()
}

inline fun <T : InvocationKind> case_2(invocationKind: T, block: () -> Unit) {
    contract { callsInPlace(block, <!ERROR_IN_CONTRACT_DESCRIPTION!>invocationKind<!>) }
    return block()
}

internal inline fun case_3(block: () -> Unit) {
    contract { callsInPlace(block, <!ERROR_IN_CONTRACT_DESCRIPTION!>invocationKind<!>) }
    return block()
}

internal inline fun case_4(block: () -> Unit) {
    contract { callsInPlace(block, <!ERROR_IN_CONTRACT_DESCRIPTION!>SampleObject.invocationKind<!>) }
    return block()
}


// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)

 SECTION: contracts
 CATEGORY: declarations, contractBuilder, common
 NUMBER: 12
 DESCRIPTION: Functions with contracts and external effect builder.
 ISSUES: KT-26186
 */

import kotlin.contracts.*

internal inline fun ContractBuilder.callsInPlaceEffectBuilder(block: () -> Unit) =
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)

internal fun ContractBuilder.returnsEffectBuilder(value_1: Int?) =
    returns(true) implies (value_1 != null)

internal inline fun case_1(block: () -> Unit) {
    contract(builder = { <!ERROR_IN_CONTRACT_DESCRIPTION!>callsInPlaceEffectBuilder(block)<!> })
    return block()
}

internal inline fun case_2(block: () -> Unit) {
    contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>callsInPlaceEffectBuilder(block)<!> }
    return block()
}

internal inline fun case_3(value_1: Int?, block: () -> Unit) {
    contract({ <!ERROR_IN_CONTRACT_DESCRIPTION!>returnsEffectBuilder(value_1)<!>; <!ERROR_IN_CONTRACT_DESCRIPTION!>callsInPlaceEffectBuilder(block)<!> })
    return block()
}

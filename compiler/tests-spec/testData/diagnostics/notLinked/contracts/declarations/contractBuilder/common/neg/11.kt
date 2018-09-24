// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)

 SECTION: contracts
 CATEGORY: declarations, contractBuilder, common
 NUMBER: 11
 DESCRIPTION: Functions with contracts and external contract builder.
 ISSUES: KT-26186
 */

import kotlin.contracts.*

internal inline fun contractBuilder(block: () -> Unit): ContractBuilder.() -> Unit = {
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
}

internal inline fun case_1(block: () -> Unit) {
    contract(<!ERROR_IN_CONTRACT_DESCRIPTION!>contractBuilder(block)<!>)
    return block()
}

internal inline fun case_2(block: () -> Unit) {
    contract(builder = <!ERROR_IN_CONTRACT_DESCRIPTION!>contractBuilder(block)<!>)
    return block()
}

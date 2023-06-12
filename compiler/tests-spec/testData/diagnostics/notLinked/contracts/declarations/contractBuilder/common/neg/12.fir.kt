// !OPT_IN: kotlin.contracts.ExperimentalContracts

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: contracts, declarations, contractBuilder, common
 * NUMBER: 12
 * DESCRIPTION: Functions with contracts and external effect builder.
 * ISSUES: KT-26186
 */

// FILE: builder.kt

package builder

import kotlin.contracts.*

// TESTCASE NUMBER: 1, 2, 3
inline fun ContractBuilder.callsInPlaceEffectBuilder(block: () -> Unit) =
    callsInPlace(<!USAGE_IS_NOT_INLINABLE!>block<!>, InvocationKind.EXACTLY_ONCE)

fun ContractBuilder.returnsEffectBuilder(value_1: Int?) =
    returns(true) implies (value_1 != null)

// FILE: main.kt

import builder.*
import kotlin.contracts.*

// TESTCASE NUMBER: 1
inline fun case_1(block: () -> Unit) {
    contract(builder = { callsInPlaceEffectBuilder(block) })
    return block()
}

// TESTCASE NUMBER: 2
inline fun case_2(block: () -> Unit) {
    contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>callsInPlaceEffectBuilder(block)<!> }
    return block()
}

// TESTCASE NUMBER: 3
inline fun case_3(value_1: Int?, block: () -> Unit) {
    contract({ returnsEffectBuilder(value_1); callsInPlaceEffectBuilder(block) })
    return block()
}

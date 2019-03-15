// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: contracts, declarations, contractBuilder, common
 * NUMBER: 11
 * DESCRIPTION: Functions with contracts and external contract builder.
 * ISSUES: KT-26186
 */

// FILE: builder.kt

package builder

import kotlin.contracts.*

// TESTCASE NUMBER: 1, 2
inline fun contractBuilder(block: () -> Unit): ContractBuilder.() -> Unit = {
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
}

// FILE: main.kt

import builder.*
import kotlin.contracts.*

// TESTCASE NUMBER: 1
inline fun case_1(block: () -> Unit) {
    contract(<!ERROR_IN_CONTRACT_DESCRIPTION!>contractBuilder(block)<!>)
    return block()
}

// TESTCASE NUMBER: 2
inline fun case_2(block: () -> Unit) {
    contract(builder = <!ERROR_IN_CONTRACT_DESCRIPTION!>contractBuilder(block)<!>)
    return block()
}

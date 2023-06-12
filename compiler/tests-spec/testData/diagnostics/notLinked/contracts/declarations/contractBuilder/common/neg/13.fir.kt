// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -UNREACHABLE_CODE -UNUSED_EXPRESSION
// !OPT_IN: kotlin.contracts.ExperimentalContracts

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: contracts, declarations, contractBuilder, common
 * NUMBER: 13
 * DESCRIPTION: Contract function with CallsInPlace effect with not allowed implies.
 * ISSUES: KT-26409
 */

import kotlin.contracts.*

// TESTCASE NUMBER: 1
fun case_1(value_1: Any?, block: () -> Unit) {
    contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>callsInPlace(block, InvocationKind.EXACTLY_ONCE) <!UNRESOLVED_REFERENCE!>implies<!> (value_1 != null)<!> }
    if (value_1 != null) block()
}

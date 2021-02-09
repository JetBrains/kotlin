// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -UNREACHABLE_CODE -UNUSED_EXPRESSION
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

// TESTCASE NUMBER: 1
fun case_1(value_1: Any?, block: () -> Unit) {
    contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>callsInPlace(block, InvocationKind.EXACTLY_ONCE) <!UNRESOLVED_REFERENCE!>implies<!> (value_1 != null)<!> }
    if (value_1 != null) block()
}

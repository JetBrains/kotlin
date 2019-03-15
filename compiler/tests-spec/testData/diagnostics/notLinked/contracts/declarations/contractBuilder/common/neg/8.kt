// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -UNREACHABLE_CODE -UNUSED_EXPRESSION
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: contracts, declarations, contractBuilder, common
 * NUMBER: 8
 * DESCRIPTION: Not allowed empty contract.
 */

import kotlin.contracts.*

// TESTCASE NUMBER: 1
inline fun case_1(block: () -> Unit) {
    <!ERROR_IN_CONTRACT_DESCRIPTION!>contract<!> { }
    return block()
}

// TESTCASE NUMBER: 2
inline fun case_2(block: () -> Unit) {
    <!ERROR_IN_CONTRACT_DESCRIPTION!>contract<!>({ })
    return block()
}

// TESTCASE NUMBER: 3
inline fun case_3(block: () -> Unit) {
    <!ERROR_IN_CONTRACT_DESCRIPTION!>contract<!>(builder = { })
    return block()
}

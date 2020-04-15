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
    contract { }
    return block()
}

// TESTCASE NUMBER: 2
inline fun case_2(block: () -> Unit) {
    contract({ })
    return block()
}

// TESTCASE NUMBER: 3
inline fun case_3(block: () -> Unit) {
    contract(builder = { })
    return block()
}

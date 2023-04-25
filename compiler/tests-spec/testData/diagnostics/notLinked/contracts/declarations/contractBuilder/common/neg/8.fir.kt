// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -UNREACHABLE_CODE -UNUSED_EXPRESSION
// !OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

// TESTCASE NUMBER: 1
inline fun case_1(block: () -> Unit) {
    <!ERROR_IN_CONTRACT_DESCRIPTION!>contract { }<!>
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

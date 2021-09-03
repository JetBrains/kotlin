// !OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

// TESTCASE NUMBER: 1
fun case_1(x: Any?): Boolean {
    contract {
        <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(true) implies (x === EmptyObject)<!> // should be not allowed
    }
    return x === EmptyObject
}

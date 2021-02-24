// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

// TESTCASE NUMBER: 1
fun case_1(x: Any?): Boolean {
    contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(true) implies (x == -.15f)<!> }
    return x !is Number
}

// TESTCASE NUMBER: 2
fun case_2(x: Any?): Boolean {
    contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(true) implies (x == "..." + ".")<!> }
    return x !is Number
}

// TESTCASE NUMBER: 3
fun case_3(x: Int, y: Int): Boolean {
    contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(true) implies (x > y)<!> }
    return x > y
}

// TESTCASE NUMBER: 4
fun case_4(x: Any?, y: Any?): Boolean {
    contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(true) implies (x == y.toString())<!> }
    return x !is Number
}

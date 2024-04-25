// OPT_IN: kotlin.contracts.ExperimentalContracts
// LANGUAGE: +UseReturnsEffect
// Issue: KT-26386

import kotlin.contracts.*

fun case_1(): Boolean {
    contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(null) implies case_1()<!> }
    return true
}

fun case_2(): Boolean {
    contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(null) implies case_3()<!> }
    return true
}

fun case_3(): Boolean {
    contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(null) implies case_2()<!> }
    return true
}

fun case_4(): Boolean {
    kotlin.contracts.contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(null) implies case_1()<!> }
    return true
}
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !LANGUAGE: +UseReturnsEffect
// Issue: KT-26386

import kotlin.contracts.*

fun case_1(): Boolean {
    contract { returns(null) implies case_1() }
    return true
}

fun case_2(): Boolean {
    contract { returns(null) implies case_3() }
    return true
}

fun case_3(): Boolean {
    contract { returns(null) implies case_2() }
    return true
}

fun case_4(): Boolean {
    kotlin.contracts.contract { returns(null) implies case_1() }
    return true
}
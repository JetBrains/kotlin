// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: contracts, declarations, contractBuilder, effects, returns
 * NUMBER: 7
 * DESCRIPTION: Returns effect with type checking with generic parameter
 * ISSUES: KT-26296
 */

import kotlin.contracts.*

// TESTCASE NUMBER: 1
fun <T : Number?> T.case_1() {
    contract { returns() implies (<!USELESS_IS_CHECK!>this@case_1 is <!ERROR_IN_CONTRACT_DESCRIPTION!>T<!><!>) }
    if (!(<!USELESS_IS_CHECK!>this@case_1 is T<!>)) throw Exception()
}

// TESTCASE NUMBER: 2
fun <T : Number, K : <!FINAL_UPPER_BOUND!>String<!>> T?.case_2(value_1: K?) {
    contract { returns() implies (this@case_2 is <!ERROR_IN_CONTRACT_DESCRIPTION!>T<!> && value_1 is K) }
    if (!(this@case_2 is T && value_1 is K)) throw Exception()
}

// TESTCASE NUMBER: 3
inline fun <reified T : Number> T?.case_3(value_1: Any?) {
    contract { returns() implies (value_1 is <!ERROR_IN_CONTRACT_DESCRIPTION!>T<!>) }
    if (!(value_1 is T)) throw Exception()
}

// TESTCASE NUMBER: 4
inline fun <reified T : Number, K> K?.case_4(value_1: Any?) {
    contract { returns() implies (this@case_4 !is <!ERROR_IN_CONTRACT_DESCRIPTION!>T<!> || value_1 is T) }
    if (!(this@case_4 !is T || value_1 is T)) throw Exception()
}

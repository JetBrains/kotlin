// LANGUAGE: +AllowContractsForNonOverridableMembers +AllowReifiedGenericsInContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts

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
    contract { returns() implies (<!USELESS_IS_CHECK!>this@case_1 is T<!>) }
    if (!(<!USELESS_IS_CHECK!>this@case_1 is T<!>)) throw Exception()
}

// TESTCASE NUMBER: 2
fun <T : Number, K : <!FINAL_UPPER_BOUND!>String<!>> T?.case_2(value_1: K?) {
    contract { returns() implies (this@case_2 is T && value_1 is K) }
    if (!(this@case_2 is T && value_1 is K)) throw Exception()
}

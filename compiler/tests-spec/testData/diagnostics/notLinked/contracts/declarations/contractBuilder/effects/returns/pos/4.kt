// FIR_IDENTICAL
// LANGUAGE: +AllowContractsForNonOverridableMembers +AllowReifiedGenericsInContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: contracts, declarations, contractBuilder, effects, returns
 * NUMBER: 4
 * DESCRIPTION: Returns effect with type checking with generic parameter
 */

import kotlin.contracts.*

// TESTCASE NUMBER: 1
inline fun <reified T : Number> T?.case_1(value_1: Any?) {
    contract { returns() implies (value_1 is T) }
    if (!(value_1 is T)) throw Exception()
}

// TESTCASE NUMBER: 2
inline fun <reified T : Number, K> K?.case_2(value_1: Any?) {
    contract { returns() implies (this@case_2 !is T || value_1 is T) }
    if (!(this@case_2 !is T || value_1 is T)) throw Exception()
}

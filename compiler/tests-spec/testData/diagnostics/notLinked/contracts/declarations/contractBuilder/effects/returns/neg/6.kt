// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: contracts, declarations, contractBuilder, effects, returns
 * NUMBER: 6
 * DESCRIPTION: Contract on the extension function with smartcast to Boolean.
 */

import kotlin.contracts.*

// TESTCASE NUMBER: 1
fun Boolean?.case_1(): Boolean {
    <!ERROR_IN_CONTRACT_DESCRIPTION!>contract<!> { returns(true) implies (this@case_1 != null && <!DEBUG_INFO_SMARTCAST!>this@case_1<!>) }
    return this != null && <!DEBUG_INFO_SMARTCAST!>this<!>
}

// TESTCASE NUMBER: 2
fun <T : <!FINAL_UPPER_BOUND!>Boolean<!>>T?.case_2(): Boolean {
    <!ERROR_IN_CONTRACT_DESCRIPTION!>contract<!> { returns(true) implies (this@case_2 != null && this@case_2 !is Nothing && <!DEBUG_INFO_SMARTCAST!>this@case_2<!>) }
    return this != null && this !is Nothing && <!DEBUG_INFO_SMARTCAST!>this<!>
}

// TESTCASE NUMBER: 3
fun <T>T?.case_3() {
    <!ERROR_IN_CONTRACT_DESCRIPTION!>contract<!> { returns() implies (this@case_3 == null || this@case_3 is Boolean? && !<!DEBUG_INFO_SMARTCAST!>this@case_3<!>) }
    if (!(this == null || this is Boolean? && !<!DEBUG_INFO_SMARTCAST!>this<!>)) throw Exception()
}

// TESTCASE NUMBER: 4
fun case_4(value_1: Boolean?): Boolean {
    <!ERROR_IN_CONTRACT_DESCRIPTION!>contract<!> { returns(true) implies (value_1 != null && !<!DEBUG_INFO_SMARTCAST!>value_1<!>) }
    return value_1 != null && !<!DEBUG_INFO_SMARTCAST!>value_1<!>
}

// TESTCASE NUMBER: 5
fun Boolean.case_5(value_1: Any?): Boolean? {
    <!ERROR_IN_CONTRACT_DESCRIPTION!>contract<!> { returnsNotNull() implies (value_1 is Boolean? && value_1 != null && <!DEBUG_INFO_SMARTCAST!>value_1<!>) }
    return if (value_1 is Boolean? && value_1 != null && <!DEBUG_INFO_SMARTCAST!>value_1<!>) true else null
}

// TESTCASE NUMBER: 6
fun Boolean?.case_6(): Boolean? {
    <!ERROR_IN_CONTRACT_DESCRIPTION!>contract<!> { returnsNotNull() implies (this@case_6 != null && <!DEBUG_INFO_SMARTCAST!>this@case_6<!>) }
    return if (this@case_6 != null && <!DEBUG_INFO_SMARTCAST!>this@case_6<!>) true else null
}

// TESTCASE NUMBER: 7
fun <T : Boolean?> T.case_7(value_1: Any?): Boolean? {
    <!ERROR_IN_CONTRACT_DESCRIPTION!>contract<!> { returnsNotNull() implies (value_1 is Boolean? && value_1 != null && <!DEBUG_INFO_SMARTCAST!>value_1<!> && this@case_7 != null && <!DEBUG_INFO_SMARTCAST!>this@case_7<!>) }
    return if (value_1 is Boolean? && value_1 != null && <!DEBUG_INFO_SMARTCAST!>value_1<!> && this@case_7 != null && <!DEBUG_INFO_SMARTCAST!>this@case_7<!>) true else null
}

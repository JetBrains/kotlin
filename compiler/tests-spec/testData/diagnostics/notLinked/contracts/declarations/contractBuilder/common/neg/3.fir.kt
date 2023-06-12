// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -UNREACHABLE_CODE -UNUSED_EXPRESSION
// !OPT_IN: kotlin.contracts.ExperimentalContracts

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: contracts, declarations, contractBuilder, common
 * NUMBER: 3
 * DESCRIPTION: contracts with not allowed complex conditions in implies.
 */

import kotlin.contracts.*

// TESTCASE NUMBER: 1
fun case_1(value_1: Boolean?): Boolean {
    contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(true) implies (value_1 != null && value_1 == false)<!> }
    return value_1 != null && value_1 == false
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Boolean, value_2: Boolean): Boolean? {
    contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>returnsNotNull() implies (value_1 != false || value_2)<!> }
    return if (value_1 != false || value_2) true else null
}

// TESTCASE NUMBER: 3
fun case_3(value_1: String?, value_2: Boolean): Boolean {
    contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(false) implies (value_1 != null && value_2 != true)<!> }
    return !(value_1 != null && value_2 != true)
}

// TESTCASE NUMBER: 4
fun case_4(value_1: Nothing?, value_2: Boolean?): Boolean? {
    contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(null) implies (<!SENSELESS_COMPARISON!>value_1 == null<!> || value_2 != null || <!SENSELESS_COMPARISON!>value_2 == false<!>)<!> }
    return if (<!SENSELESS_COMPARISON!>value_1 == null<!> || value_2 != null || <!SENSELESS_COMPARISON!>value_2 == false<!>) null else true
}

// TESTCASE NUMBER: 5
fun case_5(value_1: Any?, value_2: String?): Boolean? {
    contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(null) implies (value_1 != null && value_2 != null || value_2 == ".")<!> }
    return if (value_1 != null && value_2 != null || value_2 == ".") null else true
}

// TESTCASE NUMBER: 6
fun case_6(value_1: Boolean, value_2: Int?): Boolean? {
    contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(null) implies (value_2 == null && value_1 || value_2 == 0)<!> }
    return if (value_2 == null && value_1 || value_2 == 0) null else true
}

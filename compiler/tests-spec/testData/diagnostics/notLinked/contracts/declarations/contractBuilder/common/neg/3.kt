// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -UNREACHABLE_CODE -UNUSED_EXPRESSION
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

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
    contract { returns(true) implies (value_1 != null && <!ERROR_IN_CONTRACT_DESCRIPTION!>value_1 == false<!>) }
    return value_1 != null && value_1 == false
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Boolean, value_2: Boolean): Boolean? {
    contract { returnsNotNull() implies (<!ERROR_IN_CONTRACT_DESCRIPTION!>value_1 != false<!> || value_2) }
    return if (value_1 != false || value_2) true else null
}

// TESTCASE NUMBER: 3
fun case_3(value_1: String?, value_2: Boolean): Boolean {
    contract { returns(false) implies (value_1 != null && <!ERROR_IN_CONTRACT_DESCRIPTION!>value_2 != true<!>) }
    return !(value_1 != null && value_2 != true)
}

// TESTCASE NUMBER: 4
fun case_4(value_1: Nothing?, value_2: Boolean?): Boolean? {
    contract { returns(null) implies (<!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>value_1<!> == null<!> || value_2 != null || <!ERROR_IN_CONTRACT_DESCRIPTION!><!DEBUG_INFO_CONSTANT!>value_2<!> == false<!>) }
    return if (<!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>value_1<!> == null<!> || value_2 != null || <!DEBUG_INFO_CONSTANT!>value_2<!> == false) null else true
}

// TESTCASE NUMBER: 5
fun case_5(value_1: Any?, value_2: String?): Boolean? {
    contract { returns(null) implies (value_1 != null && value_2 != null || value_2 == <!ERROR_IN_CONTRACT_DESCRIPTION!>"."<!>) }
    return if (value_1 != null && value_2 != null || value_2 == ".") null else true
}

// TESTCASE NUMBER: 6
fun case_6(value_1: Boolean, value_2: Int?): Boolean? {
    <!ERROR_IN_CONTRACT_DESCRIPTION!>contract<!> { returns(null) implies (value_2 == null && value_1 || value_2 == 0) }
    return if (value_2 == null && value_1 || value_2 == 0) null else true
}

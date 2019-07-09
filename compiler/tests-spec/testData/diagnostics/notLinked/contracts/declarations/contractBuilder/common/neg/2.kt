// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -UNREACHABLE_CODE -UNUSED_EXPRESSION
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: contracts, declarations, contractBuilder, common
 * NUMBER: 2
 * DESCRIPTION: contracts with not allowed simple conditions in implies
 */

import kotlin.contracts.*

// TESTCASE NUMBER: 1
fun case_1(value_1: Boolean): Boolean {
    contract { returns(true) implies (<!ERROR_IN_CONTRACT_DESCRIPTION!>value_1 == true<!>) }
    return value_1 == true
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Boolean): Boolean? {
    contract { returnsNotNull() implies (<!ERROR_IN_CONTRACT_DESCRIPTION!>value_1 != false<!>) }
    return if (value_1 != false) true else null
}

// TESTCASE NUMBER: 3
fun case_3(value_1: String): Boolean {
    contract { returns(false) implies (value_1 != <!ERROR_IN_CONTRACT_DESCRIPTION!>""<!>) }
    return !(value_1 != "")
}

// TESTCASE NUMBER: 4
fun case_4(value_1: Int): Boolean? {
    <!ERROR_IN_CONTRACT_DESCRIPTION!>contract<!> { returns(null) implies (value_1 == 0) }
    return if (value_1 == 0) null else true
}

// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -UNREACHABLE_CODE -UNUSED_EXPRESSION
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !WITH_NEW_INFERENCE

import kotlin.contracts.*

// TESTCASE NUMBER: 1
fun case_1(): Boolean? {
    contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>returnsNotNull() <!INAPPLICABLE_CANDIDATE!>implies<!> (null)<!> }
    return true
}

// TESTCASE NUMBER: 2
fun case_2(): Boolean {
    contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(false) <!INAPPLICABLE_CANDIDATE!>implies<!> 0.000001<!> }
    return true
}

// TESTCASE NUMBER: 3
fun case_3(): Boolean? {
    contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(null) <!INAPPLICABLE_CANDIDATE!>implies<!> ""<!> }
    return null
}

// !OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

// TESTCASE NUMBER: 1
fun <T : <!FINAL_UPPER_BOUND!>Boolean<!>>T.case_1(): Boolean? {
    contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(null) implies (!this@case_1)<!> }
    return if (!this) null else true
}

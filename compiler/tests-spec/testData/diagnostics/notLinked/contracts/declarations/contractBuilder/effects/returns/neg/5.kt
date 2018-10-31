// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: contracts, declarations, contractBuilder, effects, returns
 * NUMBER: 5
 * DESCRIPTION: Contract on the extension function with Boolean upper bound (Boolean or Nothing) or smartcast to Boolean.
 * DISCUSSION
 */

import kotlin.contracts.*

// TESTCASE NUMBER: 1
fun <T : <!FINAL_UPPER_BOUND!>Boolean<!>>T.case_1(): Boolean? {
    <!ERROR_IN_CONTRACT_DESCRIPTION!>contract<!> { returns(null) implies (!this@case_1) }
    return if (!this) null else true
}

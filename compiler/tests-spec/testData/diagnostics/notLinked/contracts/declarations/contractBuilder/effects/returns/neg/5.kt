// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)

 SECTION: contracts
 CATEGORY: declarations, contractBuilder, effects, returns
 NUMBER: 5
 DESCRIPTION: Contract on the extension function with Boolean upper bound (Boolean or Nothing) or smartcast to Boolean.
 DISCUSSION
 */

import kotlin.contracts.*

fun <T : <!FINAL_UPPER_BOUND!>Boolean<!>>T.case_1(): Boolean? {
    <!ERROR_IN_CONTRACT_DESCRIPTION!>contract<!> { returns(null) implies (!this@case_1) }
    return if (!this) null else true
}

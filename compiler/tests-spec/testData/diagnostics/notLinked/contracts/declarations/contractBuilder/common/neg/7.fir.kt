// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -UNREACHABLE_CODE -UNUSED_EXPRESSION
// !OPT_IN: kotlin.contracts.ExperimentalContracts

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: contracts, declarations, contractBuilder, common
 * NUMBER: 7
 * DESCRIPTION: Contract function with 'this' labeled by not current extensible object
 * ISSUES: KT-26149
 * HELPERS: typesProvider
 */

import kotlin.contracts.*

// TESTCASE NUMBER: 1
fun <T> T?.case_1() {
    fun <K> K?.case_1_1(): Boolean {
        <!CONTRACT_NOT_ALLOWED!>contract<!> { <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(true) implies (this@case_1 != null)<!> }
        return this@case_1 != null
    }
}

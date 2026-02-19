// FIR_IDENTICAL
// LANGUAGE: +AllowContractsForNonOverridableMembers +AllowReifiedGenericsInContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: contracts, declarations, contractFunction
 * NUMBER: 2
 * DESCRIPTION: Check report about use contracts in literal functions, lambdas or not top-level functions.
 * HELPERS: classes
 */

import kotlin.contracts.*

// TESTCASE NUMBER: 1
object case_1 {
    fun case_1(block: () -> Unit) {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        return block()
    }
}

// TESTCASE NUMBER: 2
class case_2 : ClassLevel3() {

    fun case_2_1(number: Int?): Boolean {
        contract { returns(false) implies (number != null) }
        return number == null
    }

    fun <T>T?.case_2_2(): Boolean {
        contract { returns(false) implies (this@case_2_2 !is Number) }
        return this@case_2_2 is Number
    }
}
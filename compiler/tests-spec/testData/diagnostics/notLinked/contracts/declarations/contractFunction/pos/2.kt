// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !WITH_CLASSES

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: contracts, declarations, contractFunction
 * NUMBER: 2
 * DESCRIPTION: Use a contract in member functions
 */

import kotlin.contracts.*

// TESTCASE NUMBER: 3
object case_3 {
    fun case_3(block: () -> Unit) {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        return block()
    }
}

// TESTCASE NUMBER: 4
class case_4 : _ClassLevel3() {

    fun case_4_2(number: Int?): Boolean {
        contract { returns(false) implies (number != null) }
        return number == null
    }

    fun <T>T?.case_4_3(): Boolean {
        contract { returns(false) implies (this@case_4_3 !is Number) }
        return this@case_4_3 is Number
    }
}
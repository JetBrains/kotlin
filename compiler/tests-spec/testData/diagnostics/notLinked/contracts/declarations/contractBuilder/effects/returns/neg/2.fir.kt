// !OPT_IN: kotlin.contracts.ExperimentalContracts

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: contracts, declarations, contractBuilder, effects, returns
 * NUMBER: 2
 * DESCRIPTION: Using equality with not labeled 'this' in implies.
 */

import kotlin.contracts.*

// TESTCASE NUMBER: 1
fun Any?.case_1(): Boolean {
    contract {
        <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(true) implies (<!SENSELESS_COMPARISON!>this != null<!>)<!>
    }
    return this != null
}

// TESTCASE NUMBER: 2
fun Any?.case_2(): Boolean {
    contract {
        <!ERROR_IN_CONTRACT_DESCRIPTION!>returnsNotNull() implies (this is Number?)<!>
    }
    return this is Number?
}

// TESTCASE NUMBER: 3
fun <T> T?.case_3(): Boolean {
    contract {
        <!ERROR_IN_CONTRACT_DESCRIPTION!>returnsNotNull() implies (<!SENSELESS_COMPARISON!>this != null<!>)<!>
    }
    return this != null
}

// TESTCASE NUMBER: 4
inline fun <reified T : Number> T.case_4(): Boolean {
    contract {
        <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(null) implies (this is Int)<!>
    }
    return this is Int
}

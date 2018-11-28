// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !WITH_OBJECTS

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: contracts, declarations, contractBuilder, effects, returns
 * NUMBER: 3
 * DESCRIPTION: Using reference equality in implies.
 * ISSUES: KT-26177
 */

import kotlin.contracts.*

// TESTCASE NUMBER: 1
fun case_1(x: Any?): Boolean {
    contract {
        returns(true) implies (x === <!ERROR_IN_CONTRACT_DESCRIPTION!>_EmptyObject<!>) // should be not allowed
    }
    return x === _EmptyObject
}

// !DIAGNOSTICS: -UNUSED_VARIABLE
// !OPT_IN: kotlin.contracts.ExperimentalContracts

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: contracts, declarations, contractBuilder, effects, common
 * NUMBER: 1
 * DESCRIPTION: Indirect effect functions call.
 * ISSUES: KT-26175
 */

import kotlin.contracts.*

// TESTCASE NUMBER: 1
inline fun case_1(block: () -> Unit) {
    contract {
        <!ERROR_IN_CONTRACT_DESCRIPTION!>{ callsInPlace(block, InvocationKind.EXACTLY_ONCE) }()<!>
    }
    return block()
}

// TESTCASE NUMBER: 2
fun case_2(x: Any?): Boolean {
    contract {
         returns(true).<!ERROR_IN_CONTRACT_DESCRIPTION!>apply { implies (x is Number) }<!> // 'Returns' as result
    }
    return x is Number
}

// TESTCASE NUMBER: 3
fun case_3(x: Any?): Boolean {
    contract {
         returns(true).<!ERROR_IN_CONTRACT_DESCRIPTION!>also { it implies (x is Number) }<!> // 'Returns' as result
    }
    return x is Number
}

// TESTCASE NUMBER: 4
fun case_4(x: Any?): Boolean {
    contract {
         returns(true).<!ERROR_IN_CONTRACT_DESCRIPTION!>let { it implies (x is Number) }<!> // 'ConditionalEffect' as result
    }
    return x is Number
}

// TESTCASE NUMBER: 5
fun case_5(x: Any?): Boolean {
    contract {
        returns(true).<!ERROR_IN_CONTRACT_DESCRIPTION!>run { implies (x is Number) }<!> // 'ConditionalEffect' as result
    }
    return x is Number
}

// TESTCASE NUMBER: 6
fun case_6(x: Any?): Boolean {
    contract {
         returns(true).<!ERROR_IN_CONTRACT_DESCRIPTION!>takeIf { it implies (x is Number); false }<!> // null, must be unrecognized effect
    }
    return x is Number
}

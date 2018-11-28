// !DIAGNOSTICS: -UNUSED_VARIABLE
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !WITH_FUNCTIONS

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: contracts, declarations, contractBuilder, common
 * NUMBER: 14
 * DESCRIPTION: Contract is first statement in control flow terms, but not in tokens order terms.
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-26153
 */

import kotlin.contracts.*

// TESTCASE NUMBER: 1
inline fun case_1(block: () -> Unit) {
    return contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
}

// TESTCASE NUMBER: 2
fun case_2() = contract { }

// TESTCASE NUMBER: 3
inline fun case_3(block: () -> Unit) {
    val value_1 = contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
}

// TESTCASE NUMBER: 4
inline fun case_4(block: () -> Unit) {
    (contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    })
    return block()
}

// TESTCASE NUMBER: 5
inline fun case_5(block: () -> Unit) {
    <!REDUNDANT_LABEL_WARNING!>test@<!> contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}

// TESTCASE NUMBER: 6
inline fun case_6(block: () -> Unit) {
    throw Exception(contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }.toString())
}

// TESTCASE NUMBER: 7
inline fun case_7(block: () -> Unit) {
    _funWithAnyArg(contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    })
}

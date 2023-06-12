// !DIAGNOSTICS: -UNUSED_PARAMETER
// !OPT_IN: kotlin.contracts.ExperimentalContracts

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: contracts, declarations, contractFunction
 * NUMBER: 4
 * DESCRIPTION: Check that fun with contract and CallsInPlace effect is an inline function.
 * ISSUES: KT-27090
 */

import kotlin.contracts.*

// TESTCASE NUMBER: 1
val Boolean.case_1: () -> Unit
    get() {
        <!CONTRACT_NOT_ALLOWED!>contract<!> {
            returns() implies (this@case_1)
        }
        return {}
    }

// TESTCASE NUMBER: 2
val (() -> Unit).case_2: () -> Unit
    get() {
        <!CONTRACT_NOT_ALLOWED!>contract<!> {
            callsInPlace(this@case_2, InvocationKind.EXACTLY_ONCE)
        }
        return {}
    }

// TESTCASE NUMBER: 3
var Boolean.case_3: () -> Unit
    get() {
        return {}
    }
    set(value) {
        <!WRONG_INVOCATION_KIND!><!CONTRACT_NOT_ALLOWED!>contract<!> {
            callsInPlace(value, InvocationKind.EXACTLY_ONCE)
        }<!>
    }

// TESTCASE NUMBER: 4
var (() -> Unit).case_4: () -> Unit
    get() {
        return {}
    }
    set(value) {
        <!CONTRACT_NOT_ALLOWED!>contract<!> {
            callsInPlace(this@case_4, InvocationKind.EXACTLY_ONCE)
        }
    }

// TESTCASE NUMBER: 5
val Boolean.case_5: () -> Unit
    get() {
        <!CONTRACT_NOT_ALLOWED!>contract<!> {
            returns() implies (this@case_5)
        }

        if (!this@case_5)
            throw Exception()

        return {}
    }

// !DIAGNOSTICS: -UNUSED_VARIABLE
// !OPT_IN: kotlin.contracts.ExperimentalContracts

/*
 * ADDITION TO `KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)`
 *
 * SECTIONS: contracts, declarations, contractBuilder, common
 * NUMBER: 14
 * DESCRIPTION: Contract is first statement in control flow terms, but not in tokens order terms.
 * ISSUES: KT-26153
 * HELPERS: functions
 */

import kotlin.contracts.*

// TESTCASE NUMBER: 8
val myProp = 8
inline fun case_8(block: () -> Unit) {
    myProp
    <!CONTRACT_NOT_ALLOWED!>contract<!> {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
}

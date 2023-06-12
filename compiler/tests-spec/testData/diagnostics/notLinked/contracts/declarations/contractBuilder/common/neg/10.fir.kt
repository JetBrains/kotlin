// !OPT_IN: kotlin.contracts.ExperimentalContracts

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: contracts, declarations, contractBuilder, common
 * NUMBER: 10
 * DESCRIPTION: Contract with label after 'contract' keyword.
 * ISSUES: KT-26153
 */

import kotlin.contracts.*

// TESTCASE NUMBER: 1
inline fun case_1(block: () -> Unit) {
    contract test@ {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}

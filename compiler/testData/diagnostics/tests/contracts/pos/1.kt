// FIR_IDENTICAL
// !OPT_IN: kotlin.contracts.ExperimentalContracts

/*
 * ADDITION TO `KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)`
 *
 * SECTIONS: contracts, declarations, contractBuilder, common
 * NUMBER: 1
 * DESCRIPTION: Functions with simple contracts.
 */

import kotlin.contracts.*

// The following is an useful addition to testcases in compiler/tests-spec/testData/diagnostics/notLinked/contracts/declarations/contractBuilder/common/pos/1.kt

// TESTCASE NUMBER: 4
inline fun case_4(block: () -> Unit) {
    kotlin.contracts.contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return block()
}

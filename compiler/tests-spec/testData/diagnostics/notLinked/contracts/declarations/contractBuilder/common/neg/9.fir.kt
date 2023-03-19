// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -UNREACHABLE_CODE -UNUSED_EXPRESSION
// !OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

// TESTCASE NUMBER: 1
inline fun case_1(block: () -> Unit) = {
    <!CONTRACT_NOT_ALLOWED!>contract<!> {
        <!ERROR_IN_CONTRACT_DESCRIPTION!>callsInPlace(block, InvocationKind.EXACTLY_ONCE)<!>
    }
}

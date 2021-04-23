// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -UNREACHABLE_CODE -UNUSED_EXPRESSION
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

// TESTCASE NUMBER: 1
inline fun case_1(block: () -> Unit) = {
    contract {
        callsInPlace(<!USAGE_IS_NOT_INLINABLE!>block<!>, InvocationKind.EXACTLY_ONCE)
    }
}

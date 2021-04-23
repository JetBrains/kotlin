// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

// FILE: builder.kt

package builder

import kotlin.contracts.*

// TESTCASE NUMBER: 1, 2
inline fun contractBuilder(block: () -> Unit): ContractBuilder.() -> Unit = {
    callsInPlace(<!USAGE_IS_NOT_INLINABLE!>block<!>, InvocationKind.EXACTLY_ONCE)
}

// FILE: main.kt

import builder.*
import kotlin.contracts.*

// TESTCASE NUMBER: 1
inline fun case_1(block: () -> Unit) {
    contract(contractBuilder(block))
    return block()
}

// TESTCASE NUMBER: 2
inline fun case_2(block: () -> Unit) {
    contract(builder = contractBuilder(block))
    return block()
}

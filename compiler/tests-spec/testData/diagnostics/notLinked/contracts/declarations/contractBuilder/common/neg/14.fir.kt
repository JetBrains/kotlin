// !DIAGNOSTICS: -UNUSED_VARIABLE
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

// TESTCASE NUMBER: 1
inline fun case_1(block: () -> Unit) {
    return contract {
        callsInPlace(<!USAGE_IS_NOT_INLINABLE!>block<!>, InvocationKind.EXACTLY_ONCE)
    }
}

// TESTCASE NUMBER: 2
fun case_2() = contract { }

// TESTCASE NUMBER: 3
inline fun case_3(block: () -> Unit) {
    val value_1 = contract {
        callsInPlace(<!USAGE_IS_NOT_INLINABLE!>block<!>, InvocationKind.EXACTLY_ONCE)
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
    test@ contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}

// TESTCASE NUMBER: 6
inline fun case_6(block: () -> Unit) {
    throw Exception(contract {
        callsInPlace(<!USAGE_IS_NOT_INLINABLE!>block<!>, InvocationKind.EXACTLY_ONCE)
    }.toString())
}

// TESTCASE NUMBER: 7
inline fun case_7(block: () -> Unit) {
    funWithAnyArg(contract {
        callsInPlace(<!USAGE_IS_NOT_INLINABLE!>block<!>, InvocationKind.EXACTLY_ONCE)
    })
}

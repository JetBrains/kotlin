// KT-60004: Ensure diagnostics for `kotlin.contracts.contract()` are not raised for user-defined fun `contract()`

// !DIAGNOSTICS: -UNUSED_VARIABLE
// !OPT_IN: kotlin.contracts.ExperimentalContracts

/*
 * POSITIVE ADDITION TO `KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)`
 *
 * SECTIONS: contracts, declarations, contractBuilder, common
 * NUMBER: 14UserDefined
 * DESCRIPTION: Invocation of user-defined fun `contract()` is first statement in control flow terms, but not in tokens order terms.
 * ISSUES: KT-60004
 * HELPERS: functions
 */

import kotlin.contracts.ContractBuilder
import kotlin.contracts.InvocationKind

// User-defined fun `contract()` must not be mixed up with `kotlin.contracts.contract()`
inline fun contract(builder: ContractBuilder.() -> Unit) {}

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
        callsInPlace(<!USAGE_IS_NOT_INLINABLE!>block<!>, InvocationKind.EXACTLY_ONCE)
    })
    return block()
}

// TESTCASE NUMBER: 5
inline fun case_5(block: () -> Unit) {
    test@ contract {
        callsInPlace(<!USAGE_IS_NOT_INLINABLE!>block<!>, InvocationKind.EXACTLY_ONCE)
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

// TESTCASE NUMBER: 8
val myProp = 8
inline fun case_8(block: () -> Unit) {
    myProp
    contract {
        callsInPlace(<!USAGE_IS_NOT_INLINABLE!>block<!>, InvocationKind.EXACTLY_ONCE)
    }
}

// Copy-pasted from compiler/tests-spec/testData/diagnostics/helpers/functions.kt
fun funWithAnyArg(value_1: Any): Int {
    return value_1.hashCode()
}

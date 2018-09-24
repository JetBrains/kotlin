// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER -UNUSED_VARIABLE -UNUSED_PARAMETER -UNREACHABLE_CODE -UNUSED_EXPRESSION
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)

 SECTION: contracts
 CATEGORY: declarations, contractBuilder, common
 NUMBER: 1
 DESCRIPTION: Contract isn't first statement.
 */

import kotlin.contracts.*

inline fun case_1(block: () -> Unit) {
    val value_1 = 1
    <!CONTRACT_NOT_ALLOWED!>contract<!> { }
    return block()
}

inline fun case_2(block: () -> Unit) {
    10 - 1
    <!CONTRACT_NOT_ALLOWED!>contract<!> {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}

inline fun case_3(block: () -> Unit) {
    throw Exception()
    <!CONTRACT_NOT_ALLOWED!>contract<!> {
        callsInPlace(block, InvocationKind.UNKNOWN)
    }
    return block()
}

// это в негативные,
inline fun case_4(block: () -> Unit) {
    .0009
    return contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
}

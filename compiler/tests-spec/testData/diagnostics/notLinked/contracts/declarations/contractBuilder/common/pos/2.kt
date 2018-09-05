// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER -UNUSED_VARIABLE
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !WITH_FUNCTIONS

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)

 SECTION: contracts
 CATEGORY: declarations, contractBuilder, common
 NUMBER: 2
 DESCRIPTION: Contract is first statement in control flow terms, but not in tokens order terms.
 UNEXPECTED BEHAVIOUR
 ISSUES: KT-26153
 */

import kotlin.contracts.*

internal inline fun case_1(block: () -> Unit) {
    return contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
}

fun case_2() = contract { }

inline fun case_3(block: () -> Unit) {
    val value_1 = contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
}

inline fun case_4(block: () -> Unit) {
    (contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    })
    return block()
}

inline fun case_5(block: () -> Unit) {
    <!REDUNDANT_LABEL_WARNING!>test@<!> contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}

inline fun case_6(block: () -> Unit) {
    throw Exception(contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }.toString())
}

inline fun case_7(block: () -> Unit) {
    _funWithAnyArg(contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    })
}

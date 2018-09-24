// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)

 SECTION: contracts
 CATEGORY: declarations, contractFunction
 NUMBER: 1
 DESCRIPTION: Check that fun with contract and CallsInPlace effect is an inline function.
 UNEXPECTED BEHAVIOUR
 ISSUES: KT-26126
 */

import kotlin.contracts.*

fun funWithContractExactlyOnce(block: () -> Unit) { // report about not-inline function is expected
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return block()
}

fun case_1() {
    val value_1: Int
    funWithContractExactlyOnce { value_1 = 10 } // back-end exception
    value_1.inc()
}

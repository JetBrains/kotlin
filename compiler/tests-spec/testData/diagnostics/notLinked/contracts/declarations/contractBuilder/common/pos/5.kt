// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER -UNUSED_VARIABLE -UNUSED_PARAMETER -UNREACHABLE_CODE -UNUSED_EXPRESSION
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)

 SECTION: contracts
 CATEGORY: declarations, contractBuilder, common
 NUMBER: 5
 DESCRIPTION: Contract function with CallsInPlace effect with not allowed implies.
 UNEXPECTED BEHAVIOUR
 ISSUES: KT-26409
 */

import kotlin.contracts.*

fun case_1(value_1: Any?, block: () -> Unit) {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) implies (value_1 != null) }
    if (value_1 != null) block()
}

// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER -UNUSED_VARIABLE -UNUSED_PARAMETER -UNREACHABLE_CODE -UNUSED_EXPRESSION
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)

 SECTION: contracts
 CATEGORY: declarations, contractBuilder, common
 NUMBER: 9
 DESCRIPTION: Function with contract as returned expression.
 */

import kotlin.contracts.*

inline fun case_1(block: () -> Unit) = {
    <!CONTRACT_NOT_ALLOWED!>contract<!> {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
}
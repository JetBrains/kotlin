// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)

 SECTION: contracts
 CATEGORY: declarations, contractFunction
 NUMBER: 1
 DESCRIPTION: Check that recursion isn't allowed in contract functions with CallsInPlace effect.
 */

import kotlin.contracts.*

inline fun case_1(block: () -> Unit) {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    block()
    <!RECURSION_IN_INLINE!>case_1<!>(block)
}

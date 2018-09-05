// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER -UNUSED_VARIABLE -UNUSED_PARAMETER -UNREACHABLE_CODE -UNUSED_EXPRESSION
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)

 SECTION: contracts
 CATEGORY: declarations, contractBuilder, common
 NUMBER: 8
 DESCRIPTION: Not allowed empty contract.
 */

import kotlin.contracts.*

inline fun case_1(block: () -> Unit) {
    <!ERROR_IN_CONTRACT_DESCRIPTION!>contract<!> { }
    return block()
}

inline fun case_2(block: () -> Unit) {
    <!ERROR_IN_CONTRACT_DESCRIPTION!>contract<!>({ })
    return block()
}

inline fun case_3(block: () -> Unit) {
    <!ERROR_IN_CONTRACT_DESCRIPTION!>contract<!>(builder = { })
    return block()
}
// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)

 SECTION: contracts
 CATEGORY: declarations, contractBuilder, common
 NUMBER: 10
 DESCRIPTION: Contract with label after 'contract' keyword.
 ISSUES: KT-26153
 */

import kotlin.contracts.*

inline fun case_1(block: () -> Unit) {
    contract <!ERROR_IN_CONTRACT_DESCRIPTION!>test@ {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }<!>
    return block()
}

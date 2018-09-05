// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)

 SECTION: contracts
 CATEGORY: declarations, contractBuilder, effects, returns
 NUMBER: 4
 DESCRIPTION: Using equality with literals in implies.
 ISSUES: KT-26178
 */

import kotlin.contracts.*

fun case_1(x: Any?): Boolean {
    <!ERROR_IN_CONTRACT_DESCRIPTION!>contract<!> { returns(true) implies (x == .15f) }
    return x == .15f
}

fun case_2(x: Any?) {
    <!ERROR_IN_CONTRACT_DESCRIPTION!>contract<!> { returns() implies (x == "...") }
    if (x != "...") throw Exception()
}

fun case_3(x: Any?): Boolean {
    <!ERROR_IN_CONTRACT_DESCRIPTION!>contract<!> { returns(true) implies (x == '-') }
    return x == '-'
}

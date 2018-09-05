// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !WITH_CONTRACT_FUNCTIONS
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER -UNUSED_PARAMETER
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)

 SECTION: contracts
 CATEGORY: analysis, smartcasts
 NUMBER: 10
 DESCRIPTION: Smartcasts with correspond contract function with default value in last parameter.
 ISSUES: KT-26444
 */

// FILE: contracts.kt

package contracts

import kotlin.contracts.*

fun case_1(value_1: Int?, value_2: Int? = 10): Boolean {
    contract { returns(true) implies (value_1 != null) }
    return value_1 != null
}

fun case_2(value_1: Int? = 10, value_2: Int? = 10, value_3: Int? = 10): Boolean {
    contract { returns(true) implies (value_2 != null) }
    return value_1 != null
}

// FILE: usages.kt

import contracts.*

fun case_1(value_1: Int?) {
    if (contracts.case_1(value_1)) {
        <!DEBUG_INFO_SMARTCAST!>value_1<!>.inc()
    }
}

fun case_2(value_1: Int?) {
    if (contracts.case_2(10, value_1)) {
        <!DEBUG_INFO_SMARTCAST!>value_1<!>.inc()
    }
}

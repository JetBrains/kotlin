// !WITH_CONTRACT_FUNCTIONS
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)

 SECTION: contracts
 CATEGORIES: analysis, smartcasts
 NUMBER: 15
 DESCRIPTION: Check smartcasts working if type checking for contract function is used
 UNEXPECTED BEHAVIOUR
 ISSUES: KT-27241
 */

// FILE: contracts.kt

package contracts

import kotlin.contracts.*

fun case_1_1(cond: Boolean): Any {
    contract { returns(true) implies cond }
    return cond
}

fun case_1_2(value: Any): Boolean {
    contract { returns(true) implies (value is Boolean) }
    return value is Boolean
}

fun case_2(cond: Boolean): Any {
    contract { returns(true) implies cond }
    return cond
}

// FILE: usages.kt

import contracts.*

fun case_1(value: Any) {
    if (contracts.case_1_2(contracts.case_1_1(value is Char))) {
        println(<!DEBUG_INFO_SMARTCAST!>value<!>.category)
    }
}

fun case_2(value: Any) {
    if (contracts.case_2(value is Char) is Boolean) {
        println(<!DEBUG_INFO_SMARTCAST!>value<!>.category)
    }
}

fun case_3(value: String?) {
    if (<!USELESS_IS_CHECK!>!value.isNullOrEmpty() is Boolean<!>) {
        <!DEBUG_INFO_SMARTCAST!>value<!>.length
    }
}

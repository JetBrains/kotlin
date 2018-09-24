// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !WITH_CONTRACT_FUNCTIONS
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)

 SECTION: contracts
 CATEGORY: analysis, smartcasts
 NUMBER: 14
 DESCRIPTION: Smartcast using many of the various Returns effects on the same values.
 */

// FILE: contracts.kt

package contracts

import kotlin.contracts.*

fun <T> T?.case_1_1(): Boolean {
    contract { returns(false) implies (this@case_1_1 != null) }
    return !(this@case_1_1 != null)
}

fun <T> T?.case_1_2(): Boolean? {
    contract { returns(null) implies (this@case_1_2 is String) }
    return if (this@case_1_2 is String) null else true
}

fun <T> T?.case_2_1(): Boolean {
    contract { returns(true) implies (this@case_2_1 is Float) }
    return this@case_2_1 is Float
}

fun <T> T?.case_2_2(): Boolean {
    contract { returns(false) implies (this@case_2_2 is Double) }
    return !(this@case_2_2 is Double)
}

// FILE: usages.kt

import contracts.*

fun case_1(value_1: Any?) {
    if (!(value_1.case_1_1() || value_1.case_1_2() == null)) {
        println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
    }
}

/*
 UNEXPECTED BEHAVIOUR
 ISSUES: KT-1982
 */
fun case_2(value_1: Any?) {
    if (value_1.case_2_1() || !value_1.case_2_2()) {
        println(value_1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
    }
}

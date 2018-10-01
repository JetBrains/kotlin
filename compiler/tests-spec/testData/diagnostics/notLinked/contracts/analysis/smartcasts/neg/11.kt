// !WITH_CONTRACT_FUNCTIONS
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)

 SECTION: contracts
 CATEGORIES: analysis, smartcasts
 NUMBER: 11
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

fun <T : Number?> T.case_2_1(): Boolean? {
    contract { returnsNotNull() implies (this@case_2_1 != null) }
    return if (this@case_2_1 != null) true else null
}
fun <T : Number?> T.case_2_2(): Boolean? {
    contract { returns(null) implies (this@case_2_2 != null) }
    return if (this@case_2_2 != null) null else true
}

// FILE: usages.kt

import contracts.*

fun case_1(value_1: Any?) {
    if (!(value_1.case_1_1() || value_1.case_1_2() == null)) {
        println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
    }
}

// DISCUSSION: maybe make unreachable code in the second condition?
fun case_2(value_1: Number?) {
    if (value_1?.case_2_1() != null) println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.toByte())
    if (value_1?.case_2_2() != null) println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.toByte())
}
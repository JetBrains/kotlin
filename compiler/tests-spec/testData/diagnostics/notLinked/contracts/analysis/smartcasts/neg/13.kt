// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)

 SECTION: contracts
 CATEGORY: analysis, smartcasts
 NUMBER: 13
 DESCRIPTION: Check smartcast to upper bound of the types in disjunction.
 UNEXPECTED BEHAVIOUR
 ISSUES: KT-1982
 */

// FILE: contracts.kt

package contracts

import kotlin.contracts.*

fun <T : Any?> T?.case_1() {
    contract { returns() implies (this@case_1 is Number || this@case_1 is Int) }
    if (!(this@case_1 is Number || this@case_1 is Int)) throw Exception()
}

inline fun <reified T : Any?> T?.case_2(value_2: Number, value_3: Any?, value_4: String?) {
    contract { returns() implies ((this@case_2 is Number || this@case_2 is Int) && value_2 is Int && value_3 != null && value_3 is Number && value_4 != null) }
    if (!((this is Number || this is Int) && value_2 is Int && value_3 != null && value_3 is Number && value_4 != null)) throw Exception()
}

// FILE: usages.kt

import contracts.*

fun case_1(value_1: Any?) {
    value_1.case_1()
    println(value_1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
}

fun case_2(value_1: Any?, value_2: Number, value_3: Any?, value_4: String?) {
    value_1.case_2(value_2, value_3, value_4)
    println(value_1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
}

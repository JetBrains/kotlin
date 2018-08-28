// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)

 SECTION: contracts
 CATEGORY: declarations, contractBuilder, effects, returns
 NUMBER: 3
 DESCRIPTION: Returns effect with conditions on receiver.
 */

import kotlin.contracts.*

fun Any?.case_1(): Boolean {
    contract { returns(false) implies (this@case_1 != null) }
    return this == null
}

fun <T> T?.case_2(value_1: Any?, value_2: Any?) {
    contract { returns() implies (this@case_2 is String? || value_1 !is Int && value_2 !is Nothing?) }
    if (!(this@case_2 is String? || value_1 !is Int && value_2 !is Nothing?)) throw Exception()
}

inline fun <reified T : Number?> T.case_3(value_1: Any?) {
    contract { returns() implies (value_1 == null || this@case_3 != null && this@case_3 is Int) }
    if (!(value_1 == null || this@case_3 != null && this@case_3 is Int)) throw Exception()
}

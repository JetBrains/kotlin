// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER -FINAL_UPPER_BOUND
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !WITH_CLASSES

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)

 SECTION: contracts
 CATEGORY: declarations, contractBuilder, effects, returns
 NUMBER: 2
 DESCRIPTION: Returns effect with complex conditions (using conjunction and disjuntion).
 */

import kotlin.contracts.*

fun case_1(cond1: Boolean, cond2: Boolean, cond3: Boolean) {
    contract { returns() implies (cond1 && !cond2 || cond3) }
    if (!(cond1 && !cond2 || cond3)) throw Exception()
}

fun case_2(value_1: Any?, value_2: Any?, value_3: Any?) {
    contract { returns() implies (value_1 is String? || value_2 !is Int && value_3 !is Nothing?) }
    if (!(value_1 is String? || value_2 !is Int && value_3 !is Nothing?)) throw Exception()
}

fun case_3(value_1: Any?, value_2: Any?, value_3: Any?) {
    contract { returns() implies (value_1 == null || value_2 != null && value_3 == null) }
    if (!(value_1 == null || value_2 != null && value_3 == null)) throw Exception()
}

fun <T>T.case_4(): Boolean {
    contract { returns(false) implies (this@case_4 is Char || this@case_4 == null) }
    return !(this is Char || this == null)
}

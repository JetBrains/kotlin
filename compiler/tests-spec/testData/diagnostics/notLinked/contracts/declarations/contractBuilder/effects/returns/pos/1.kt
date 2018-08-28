// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER -FINAL_UPPER_BOUND
// !WITH_CLASSES

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)

 SECTION: contracts
 CATEGORY: declarations, contractBuilder, effects, returns
 NUMBER: 1
 DESCRIPTION: Returns effect with simple conditions.
 */

import kotlin.contracts.*

fun case_1(value_1: Boolean) {
    contract { returns() implies (value_1) }
    if (!value_1) throw Exception()
}

fun case_2(value_1: Boolean): Boolean {
    contract { returns(false) implies (!value_1) }
    return value_1
}

fun Boolean.case_3() {
    contract { returns() implies (!this@case_3) }
    if (this@case_3) throw Exception()
}

fun case_5(value_1: Any?) {
    contract { returns() implies (value_1 is String) }
    if (value_1 !is String) throw Exception()
}

fun case_6(value_1: Any?) {
    contract { returns() implies (value_1 !is String?) }
    if (value_1 is String?) throw Exception()
}

fun Any?.case_7() {
    contract { returns() implies (this@case_7 is Number) }
    if (this !is Number) throw Exception()
}

fun <T>T?.case_8() {
    contract { returns() implies (this@case_8 !is _ClassLevel3?) }
    if (this is _ClassLevel3?) throw Exception()
}

fun <T : Number?>T.case_9(): Boolean? {
    contract { returns(null) implies (this@case_9 is Byte?) }
    return if (this is Byte?) null else true
}

fun case_10(value_1: Any?) {
    contract { returns() implies (value_1 == null) }
    if (value_1 != null) throw Exception()
}

fun case_11(value_1: Any?): Boolean? {
    contract { returns(null) implies (value_1 != null) }
    return if (value_1 != null) null else true
}

fun Char.case_12() {
    contract { returns() implies (<!SENSELESS_COMPARISON!>this@case_12 == null<!>) }
    if (<!SENSELESS_COMPARISON!>this@case_12 != null<!>) throw Exception()
}

fun <T : Number>T?.case_13() {
    contract { returns() implies (this@case_13 == null) }
    if (this != null) throw Exception()
}

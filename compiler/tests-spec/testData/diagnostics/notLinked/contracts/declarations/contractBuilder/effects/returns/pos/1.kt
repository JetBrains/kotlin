// FIR_IDENTICAL
// !OPT_IN: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -FINAL_UPPER_BOUND

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: contracts, declarations, contractBuilder, effects, returns
 * NUMBER: 1
 * DESCRIPTION: Returns effect with simple conditions.
 * HELPERS: classes
 */

import kotlin.contracts.*

// TESTCASE NUMBER: 1
fun case_1(value_1: Boolean) {
    contract { returns() implies (value_1) }
    if (!value_1) throw Exception()
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Boolean): Boolean {
    contract { returns(false) implies (!value_1) }
    return value_1
}

// TESTCASE NUMBER: 3
fun Boolean.case_3() {
    contract { returns() implies (!this@case_3) }
    if (this@case_3) throw Exception()
}

// TESTCASE NUMBER: 5
fun case_5(value_1: Any?) {
    contract { returns() implies (value_1 is String) }
    if (value_1 !is String) throw Exception()
}

// TESTCASE NUMBER: 6
fun case_6(value_1: Any?) {
    contract { returns() implies (value_1 !is String?) }
    if (value_1 is String?) throw Exception()
}

// TESTCASE NUMBER: 7
fun Any?.case_7() {
    contract { returns() implies (this@case_7 is Number) }
    if (this !is Number) throw Exception()
}

// TESTCASE NUMBER: 8
fun <T>T?.case_8() {
    contract { returns() implies (this@case_8 !is ClassLevel3?) }
    if (this is ClassLevel3?) throw Exception()
}

// TESTCASE NUMBER: 9
fun <T : Number?>T.case_9(): Boolean? {
    contract { returns(null) implies (this@case_9 is Byte?) }
    return if (this is Byte?) null else true
}

// TESTCASE NUMBER: 10
fun case_10(value_1: Any?) {
    contract { returns() implies (value_1 == null) }
    if (value_1 != null) throw Exception()
}

// TESTCASE NUMBER: 11
fun case_11(value_1: Any?): Boolean? {
    contract { returns(null) implies (value_1 != null) }
    return if (value_1 != null) null else true
}

// TESTCASE NUMBER: 12
fun Char.case_12() {
    contract { returns() implies (<!SENSELESS_COMPARISON!>this@case_12 == null<!>) }
    if (<!SENSELESS_COMPARISON!>this@case_12 != null<!>) throw Exception()
}

// TESTCASE NUMBER: 13
fun <T : Number>T?.case_13() {
    contract { returns() implies (this@case_13 == null) }
    if (this != null) throw Exception()
}

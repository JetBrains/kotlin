// !WITH_CONTRACT_FUNCTIONS
// !WITH_BASIC_TYPES
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: contracts, analysis, smartcasts
 * NUMBER: 9
 * DESCRIPTION: Smartcast using complex condition with some contract functions (Returns effect).
 */

// FILE: contracts.kt

package contracts

import kotlin.contracts.*

// TESTCASE NUMBER: 4
fun <T> T?.case_4(): Boolean {
    contract { returns(true) implies (this@case_4 != null) }
    return this@case_4 != null
}
fun <T> T?.case_4_1(): Boolean {
    contract { returns(false) implies (this@case_4_1 != null) }
    return !(this@case_4_1 != null)
}
fun <T> T?.case_4_2(): Boolean? {
    contract { returns(null) implies (this@case_4_2 is String) }
    return if (this@case_4_2 is String) null else true
}

// TESTCASE NUMBER: 11
fun <T> T?.case_11_1(): Boolean {
    contract { returns(false) implies (this@case_11_1 != null) }
    return !(this@case_11_1 != null)
}
fun <T> T?.case_11_2(): Boolean? {
    contract { returns(null) implies (this@case_11_2 is String) }
    return if (this@case_11_2 is String) null else true
}

// TESTCASE NUMBER: 12
fun <T> T?.case_12(): Boolean {
    contract { returns(false) implies (this@case_12 is String) }
    return if (this@case_12 is String) false else true
}

// FILE: main.kt

import contracts.*

// TESTCASE NUMBER: 1
fun case_1(value_1: Any?) {
    if (funWithReturnsTrue(value_1 is String) && funWithReturnsTrueAndNotNullCheck(value_1)) {
        println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
    }
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Any?) {
    if (!funWithReturnsFalse(value_1 is String) && !funWithReturnsTrueAndNullCheck(value_1)) {
        println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
    }
}

// TESTCASE NUMBER: 3
fun case_3(value_1: Any?) {
    if (funWithReturnsNull(value_1 is String?) == null && funWithReturnsTrue(value_1 != null)) {
        println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
    }
}

// TESTCASE NUMBER: 4
fun case_4(value_1: Any?) {
    if (!value_1.case_4_1() && value_1.case_4_2() == null) {
        println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
    }
}

// TESTCASE NUMBER: 5
fun case_5(value_1: Any?, value_2: Boolean) {
    if (!funWithReturnsFalse(value_1 is String) && value_2) {
        println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
    }
}

// TESTCASE NUMBER: 6
fun case_6(value_1: Any?, value_2: Boolean?) {
    if (funWithReturnsNull(value_1 is String) == null && value_2 != null && <!DEBUG_INFO_SMARTCAST!>value_2<!>) {
        println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
    }
}

// TESTCASE NUMBER: 7
fun case_7(value_1: String?) {
    if (funWithReturnsTrueAndNotNullCheck(value_1) && true) {
        println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
    }
}

// TESTCASE NUMBER: 8
fun case_8(value_1: Any?) {
    if (funWithReturnsTrueAndNullCheck(value_1) && false) {
        println(<!DEBUG_INFO_CONSTANT!>value_1<!>)
    }
}

// TESTCASE NUMBER: 9
fun case_9(value_1: Any?) {
    if (funWithReturnsFalse(value_1 is String) || funWithReturnsFalse(value_1 is Int)) {

    } else {
        println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
        println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.inv())
    }
}

// TESTCASE NUMBER: 10
fun case_10(value_1: Any?) {
    if (funWithReturnsFalse(value_1 is String) || getBoolean()) {

    } else {
        println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
    }
}

// TESTCASE NUMBER: 11
fun case_11(value_1: Any?) {
    if (!(value_1.case_11_1() || value_1.case_11_2() != null)) {
        println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
    }
}

// TESTCASE NUMBER: 12
fun case_12(value_1: Any?) {
    if (!value_1.case_12() || !value_1.case_12()) {
        println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
    }
    if (!value_1.case_12() && !<!DEBUG_INFO_SMARTCAST!>value_1<!>.case_12()) {
        println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
    }
}

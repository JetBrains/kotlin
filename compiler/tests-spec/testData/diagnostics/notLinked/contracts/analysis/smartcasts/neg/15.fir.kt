// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !WITH_NEW_INFERENCE

// FILE: contracts.kt

package contracts

import kotlin.contracts.*

// TESTCASE NUMBER: 1
fun case_1_1(cond: Boolean): Any {
    contract { returns(true) implies cond }
    return cond
}
fun case_1_2(value: Any): Boolean {
    contract { returns(true) implies (value is Boolean) }
    return value is Boolean
}

// TESTCASE NUMBER: 2
fun case_2(cond: Boolean): Any {
    contract { returns(true) implies cond }
    return cond
}

// FILE: main.kt

import contracts.*

// TESTCASE NUMBER: 1
fun case_1(value: Any) {
    if (contracts.case_1_2(contracts.case_1_1(value is Char))) {
        println(value.<!INAPPLICABLE_CANDIDATE!>category<!>)
    }
}

// TESTCASE NUMBER: 2
fun case_2(value: Any) {
    if (contracts.case_2(value is Char) is Boolean) {
        println(value.<!INAPPLICABLE_CANDIDATE!>category<!>)
    }
}

// TESTCASE NUMBER: 3
fun case_3(value: String?) {
    if (!value.isNullOrEmpty() is Boolean) {
        value.<!INAPPLICABLE_CANDIDATE!>length<!>
    }
}

// !OPT_IN: kotlin.contracts.ExperimentalContracts

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: contracts, analysis, smartcasts
 * NUMBER: 7
 * DESCRIPTION: Smartcasts using Returns effects with nested or subsequent contract function calls.
 */

// FILE: contracts.kt

package contracts

import kotlin.contracts.*

// TESTCASE NUMBER: 1
fun case_1_1(value_1: Int?) {
    contract { returns() implies (value_1 != null) }
    if (!(value_1 != null)) throw Exception()
}
fun case_1_2(value_1: Int?) {
    contract { returns() implies (value_1 == null) }
    if (!(value_1 == null)) throw Exception()
}

// TESTCASE NUMBER: 2
fun case_2_1(value_1: Number?) {
    contract { returns() implies (value_1 is Float) }
    if (!(value_1 is Float)) throw Exception()
}
fun case_2_2(value_1: Number?) {
    contract { returns() implies (value_1 is Int) }
    if (!(value_1 is Int)) throw Exception()
}

// TESTCASE NUMBER: 3
fun case_3_1(value_1: Any?) {
    contract { returns() implies (value_1 is String) }
    if (!(value_1 is String)) throw Exception()
}
fun case_3_2(value_1: Any?) {
    contract { returns() implies (value_1 !is String) }
    if (!(value_1 !is String)) throw Exception()
}

// TESTCASE NUMBER: 4
fun case_4_1(value_1: Any?) {
    contract { returns() implies (value_1 is Number?) }
    if (!(value_1 is Number?)) throw Exception()
}
fun case_4_2(value_1: Number?) {
    contract { returns() implies (value_1 != null) }
    if (!(value_1 != null)) throw Exception()
}
fun case_4_3(value_1: Number) {
    contract { returns() implies (value_1 is Int) }
    if (!(value_1 is Int)) throw Exception()
}

// TESTCASE NUMBER: 5
fun case_5_1(value_1: Int?): Boolean {
    contract { returns(true) implies (value_1 != null) }
    return value_1 != null
}
fun case_5_2(value_1: Int?): Boolean {
    contract { returns(true) implies (value_1 == null) }
    return value_1 == null
}
fun case_5_3(value_1: Int?): Boolean {
    contract { returns(false) implies (value_1 != null) }
    return !(value_1 != null)
}
fun case_5_4(value_1: Int?): Boolean {
    contract { returns(false) implies (value_1 == null) }
    return !(value_1 == null)
}
fun case_5_5(value_1: Int?): Boolean? {
    contract { returnsNotNull() implies (value_1 != null) }
    return if (value_1 != null) true else null
}
fun case_5_6(value_1: Int?): Boolean? {
    contract { returnsNotNull() implies (value_1 == null) }
    return if (value_1 == null) true else null
}
fun case_5_7(value_1: Int?): Boolean? {
    contract { returns(null) implies (value_1 != null) }
    return if (value_1 != null) null else true
}
fun case_5_8(value_1: Int?): Boolean? {
    contract { returns(null) implies (value_1 == null) }
    return if (value_1 == null) null else true
}

// TESTCASE NUMBER: 6
fun case_6_1(value_1: Number?): Boolean {
    contract { returns(true) implies (value_1 is Float) }
    return value_1 is Float
}
fun case_6_2(value_1: Number?): Boolean {
    contract { returns(true) implies (value_1 is Int) }
    return value_1 is Int
}
fun case_6_3(value_1: Number?): Boolean {
    contract { returns(false) implies (value_1 is Float) }
    return !(value_1 is Float)
}
fun case_6_4(value_1: Number?): Boolean {
    contract { returns(false) implies (value_1 is Int) }
    return !(value_1 is Int)
}
fun case_6_5(value_1: Number?): Boolean? {
    contract { returnsNotNull() implies (value_1 is Float) }
    return if (value_1 is Float) true else null
}
fun case_6_6(value_1: Number?): Boolean? {
    contract { returnsNotNull() implies (value_1 is Int) }
    return if (value_1 is Int) true else null
}
fun case_6_7(value_1: Number?): Boolean? {
    contract { returns(null) implies (value_1 is Float) }
    return if (value_1 is Float) null else true
}
fun case_6_8(value_1: Number?): Boolean? {
    contract { returns(null) implies (value_1 is Int) }
    return if (value_1 is Int) null else true
}

// TESTCASE NUMBER: 7
fun case_7_1(value_1: Any?): Boolean {
    contract { returns(true) implies (value_1 is String) }
    return value_1 is String
}
fun case_7_2(value_1: Any?): Boolean {
    contract { returns(true) implies (value_1 !is String) }
    return value_1 !is String
}
fun case_7_3(value_1: Any?): Boolean {
    contract { returns(false) implies (value_1 is String) }
    return !(value_1 is String)
}
fun case_7_4(value_1: Any?): Boolean {
    contract { returns(false) implies (value_1 !is String) }
    return !(value_1 !is String)
}
fun case_7_5(value_1: Any?): Boolean? {
    contract { returnsNotNull() implies (value_1 is String) }
    return if (value_1 is String) true else null
}
fun case_7_6(value_1: Any?): Boolean? {
    contract { returnsNotNull() implies (value_1 !is String) }
    return if (value_1 !is String) true else null
}
fun case_7_7(value_1: Any?): Boolean? {
    contract { returns(null) implies (value_1 is String) }
    return if (value_1 is String) null else true
}
fun case_7_8(value_1: Any?): Boolean? {
    contract { returns(null) implies (value_1 !is String) }
    return if (value_1 !is String) null else true
}

// TESTCASE NUMBER: 8
fun case_8_1(value_1: Any?): Boolean {
    contract { returns(true) implies (value_1 is Number?) }
    return value_1 is Number?
}
fun case_8_2(value_1: Number?): Boolean {
    contract { returns(true) implies (value_1 != null) }
    return value_1 != null
}
fun case_8_3(value_1: Number): Boolean {
    contract { returns(true) implies (value_1 is Int) }
    return value_1 is Int
}
fun case_8_4(value_1: Any?): Boolean {
    contract { returns(false) implies (value_1 is Number?) }
    return !(value_1 is Number?)
}
fun case_8_5(value_1: Number?): Boolean {
    contract { returns(false) implies (value_1 != null) }
    return !(value_1 != null)
}
fun case_8_6(value_1: Number): Boolean {
    contract { returns(false) implies (value_1 is Int) }
    return !(value_1 is Int)
}
fun case_8_7(value_1: Any?): Boolean? {
    contract { returnsNotNull() implies (value_1 is Number?) }
    return if (value_1 is Number?) true else null
}
fun case_8_8(value_1: Number?): Boolean? {
    contract { returnsNotNull() implies (value_1 != null) }
    return if (value_1 != null) true else null
}
fun case_8_9(value_1: Number): Boolean? {
    contract { returnsNotNull() implies (value_1 is Int) }
    return if (value_1 is Int) true else null
}
fun case_8_10(value_1: Any?): Boolean? {
    contract { returns(null) implies (value_1 is Number?) }
    return if (value_1 is Number?) null else true
}
fun case_8_11(value_1: Number?): Boolean? {
    contract { returns(null) implies (value_1 != null) }
    return if (value_1 != null) null else true
}
fun case_8_12(value_1: Number): Boolean? {
    contract { returns(null) implies (value_1 is Int) }
    return if (value_1 is Int) null else true
}

// FILE: main.kt

import contracts.*

// TESTCASE NUMBER: 1
fun case_1(value_1: Int?) {
    case_1_1(value_1)
    value_1.inv()
    case_1_2(value_1)
    value_1.inv()
    case_1_1(value_1)
    value_1.inv()
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Number?) {
    case_2_1(value_1)
    value_1.<!DEPRECATION_ERROR!>toByte<!>()
    case_2_2(value_1)
    value_1.inv()
}

// TESTCASE NUMBER: 3
fun case_3(value_1: Any?) {
    case_3_1(value_1)
    value_1.length
    case_3_2(value_1)
    value_1.length
}

// TESTCASE NUMBER: 4
fun case_4(value_1: Any?) {
    case_4_1(value_1)
    value_1?.toByte()
    case_4_2(value_1)
    value_1.toByte()
    case_4_3(value_1)
    value_1.inv()
}

// TESTCASE NUMBER: 5
fun case_5(value_1: Int?, value_2: Int?) {
    if (case_5_1(value_1)) {
        value_1.inv()
        if (case_5_2(value_1)) {
            value_1.inv()
            value_1.inv()
        }
    }
    if (!case_5_3(value_2)) {
        value_2.inv()
        if (!case_5_4(value_2)) {
            value_2.inv()
            value_2.inv()
        }
    }
    if (case_5_5(value_2) != null) {
        value_2.inv()
        if (case_5_6(value_2) != null) {
            value_2.inv()
            value_2.inv()
        }
    }
    if (case_5_7(value_2) == null) {
        value_2.inv()
        if (case_5_8(value_2) == null) {
            value_2.inv()
            value_2.inv()
        }
    }
}

// TESTCASE NUMBER: 6
fun case_6(value_1: Number?, value_2: Number?) {
    when {
        case_6_1(value_1) -> {
            value_1.<!DEPRECATION_ERROR!>toByte<!>()
            when { case_6_2(value_1) -> value_1.inv() }
        }
    }
    when {
        !case_6_3(value_2) -> {
            value_2.<!DEPRECATION_ERROR!>toByte<!>()
            when { !case_6_4(value_2) -> value_2.inv() }
        }
    }
    when {
        case_6_5(value_2) != null -> {
            value_2.<!DEPRECATION_ERROR!>toByte<!>()
            when { case_6_6(value_2) != null -> value_2.inv() }
        }
    }
    when {
        case_6_7(value_2) == null -> {
            value_2.<!DEPRECATION_ERROR!>toByte<!>()
            when { case_6_8(value_2) == null -> value_2.inv() }
        }
    }
}

// TESTCASE NUMBER: 7
fun case_7(value_1: Any?, value_2: Any?) {
    if (case_7_1(value_1)) {
        value_1.length
        if (case_7_2(value_1)) value_1.length
    }
    if (!case_7_3(value_2)) {
        value_2.length
        if (!case_7_4(value_2)) value_2.length
    }
    if (case_7_5(value_2) != null) {
        value_2.length
        if (case_7_6(value_2) != null) value_2.length
    }
    if (case_7_7(value_2) == null) {
        value_2.length
        if (case_7_8(value_2) == null) value_2.length
    }
}

// TESTCASE NUMBER: 8
fun case_8(value_1: Any?, value_2: Any?) {
    if (case_8_1(value_1)) {
        value_1?.toByte()
        if (case_8_2(value_1)) {
            value_1.toByte()
            if (case_8_3(value_1)) value_1.inv()
        }
    }
    if (!case_8_4(value_2)) {
        value_2?.toByte()
        if (!case_8_5(value_2)) {
            value_2.toByte()
            if (!case_8_6(value_2)) value_2.inv()
        }
    }
    if (case_8_7(value_2) != null) {
        value_2?.toByte()
        if (case_8_8(value_2) != null) {
            value_2.toByte()
            if (case_8_9(value_2) != null) value_2.inv()
        }
    }
    if (case_8_10(value_2) == null) {
        value_2?.toByte()
        if (case_8_11(value_2) == null) {
            value_2.toByte()
            if (case_8_12(value_2) == null) value_2.inv()
        }
    }
}

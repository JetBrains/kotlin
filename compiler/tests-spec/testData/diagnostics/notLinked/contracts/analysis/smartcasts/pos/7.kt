// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)

 SECTION: contracts
 CATEGORY: analysis, smartcasts
 NUMBER: 7
 DESCRIPTION: Smartcasts using Returns effects with nested or subsequent contract function calls.
 */

// FILE: contracts.kt

package contracts

import kotlin.contracts.*

fun case_1_1(value_1: Int?) {
    contract { returns() implies (value_1 != null) }
    if (!(value_1 != null)) throw Exception()
}
fun case_1_2(value_1: Int?) {
    contract { returns() implies (value_1 == null) }
    if (!(value_1 == null)) throw Exception()
}

fun case_2_1(value_1: Number?) {
    contract { returns() implies (value_1 is Float) }
    if (!(value_1 is Float)) throw Exception()
}
fun case_2_2(value_1: Number?) {
    contract { returns() implies (value_1 is Int) }
    if (!(value_1 is Int)) throw Exception()
}

fun case_3_1(value_1: Any?) {
    contract { returns() implies (value_1 is String) }
    if (!(value_1 is String)) throw Exception()
}
fun case_3_2(value_1: Any?) {
    contract { returns() implies (value_1 !is String) }
    if (!(value_1 !is String)) throw Exception()
}

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

// FILE: usages.kt

import contracts.*

fun case_1(value_1: Int?) {
    case_1_1(value_1)
    <!DEBUG_INFO_SMARTCAST!>value_1<!>.inv()
    case_1_2(value_1)
    <!DEBUG_INFO_SMARTCAST!>value_1<!>.<!UNREACHABLE_CODE!>inv()<!>
    <!UNREACHABLE_CODE!>case_1_1(value_1)<!>
    <!UNREACHABLE_CODE!><!DEBUG_INFO_SMARTCAST!>value_1<!>.inv()<!>
}

fun case_2(value_1: Number?) {
    case_2_1(value_1)
    <!DEBUG_INFO_SMARTCAST!>value_1<!>.toByte()
    case_2_2(value_1)
    <!DEBUG_INFO_SMARTCAST!>value_1<!>.inv()
}

fun case_3(value_1: Any?) {
    case_3_1(value_1)
    <!DEBUG_INFO_SMARTCAST!>value_1<!>.length
    case_3_2(value_1)
    <!DEBUG_INFO_SMARTCAST!>value_1<!>.length
}

fun case_4(value_1: Any?) {
    case_4_1(value_1)
    <!DEBUG_INFO_SMARTCAST!>value_1<!>?.toByte()
    case_4_2(<!DEBUG_INFO_SMARTCAST!>value_1<!>)
    <!DEBUG_INFO_SMARTCAST!>value_1<!>.toByte()
    case_4_3(<!DEBUG_INFO_SMARTCAST!>value_1<!>)
    <!DEBUG_INFO_SMARTCAST!>value_1<!>.inv()
}

fun case_5(value_1: Int?, value_2: Int?) {
    if (case_5_1(value_1)) {
        <!DEBUG_INFO_SMARTCAST!>value_1<!>.inv()
        if (case_5_2(value_1)) {
            <!DEBUG_INFO_SMARTCAST!>value_1<!>.<!UNREACHABLE_CODE!>inv()<!>
            <!UNREACHABLE_CODE!><!DEBUG_INFO_SMARTCAST!>value_1<!>.inv()<!>
        }
    }
    if (!case_5_3(value_2)) {
        <!DEBUG_INFO_SMARTCAST!>value_2<!>.inv()
        if (!case_5_4(value_2)) {
            <!DEBUG_INFO_SMARTCAST!>value_2<!>.<!UNREACHABLE_CODE!>inv()<!>
            <!UNREACHABLE_CODE!><!DEBUG_INFO_SMARTCAST!>value_2<!>.inv()<!>
        }
    }
    if (case_5_5(value_2) != null) {
        <!DEBUG_INFO_SMARTCAST!>value_2<!>.inv()
        if (case_5_6(value_2) != null) {
            <!DEBUG_INFO_SMARTCAST!>value_2<!>.<!UNREACHABLE_CODE!>inv()<!>
            <!UNREACHABLE_CODE!><!DEBUG_INFO_SMARTCAST!>value_2<!>.inv()<!>
        }
    }
    if (case_5_7(value_2) == null) {
        <!DEBUG_INFO_SMARTCAST!>value_2<!>.inv()
        if (case_5_8(value_2) == null) {
            <!DEBUG_INFO_SMARTCAST!>value_2<!>.<!UNREACHABLE_CODE!>inv()<!>
            <!UNREACHABLE_CODE!><!DEBUG_INFO_SMARTCAST!>value_2<!>.inv()<!>
        }
    }
}

fun case_6(value_1: Number?, value_2: Number?) {
    when {
        case_6_1(value_1) -> {
            <!DEBUG_INFO_SMARTCAST!>value_1<!>.toByte()
            when { case_6_2(value_1) -> <!DEBUG_INFO_SMARTCAST!>value_1<!>.inv() }
        }
    }
    when {
        !case_6_3(value_2) -> {
            <!DEBUG_INFO_SMARTCAST!>value_2<!>.toByte()
            when { !case_6_4(value_2) -> <!DEBUG_INFO_SMARTCAST!>value_2<!>.inv() }
        }
    }
    when {
        case_6_5(value_2) != null -> {
            <!DEBUG_INFO_SMARTCAST!>value_2<!>.toByte()
            when { case_6_6(value_2) != null -> <!DEBUG_INFO_SMARTCAST!>value_2<!>.inv() }
        }
    }
    when {
        case_6_7(value_2) == null -> {
            <!DEBUG_INFO_SMARTCAST!>value_2<!>.toByte()
            when { case_6_8(value_2) == null -> <!DEBUG_INFO_SMARTCAST!>value_2<!>.inv() }
        }
    }
}

fun case_7(value_1: Any?, value_2: Any?) {
    if (case_7_1(value_1)) {
        <!DEBUG_INFO_SMARTCAST!>value_1<!>.length
        if (case_7_2(value_1)) <!DEBUG_INFO_SMARTCAST!>value_1<!>.length
    }
    if (!case_7_3(value_2)) {
        <!DEBUG_INFO_SMARTCAST!>value_2<!>.length
        if (!case_7_4(value_2)) <!DEBUG_INFO_SMARTCAST!>value_2<!>.length
    }
    if (case_7_5(value_2) != null) {
        <!DEBUG_INFO_SMARTCAST!>value_2<!>.length
        if (case_7_6(value_2) != null) <!DEBUG_INFO_SMARTCAST!>value_2<!>.length
    }
    if (case_7_7(value_2) == null) {
        <!DEBUG_INFO_SMARTCAST!>value_2<!>.length
        if (case_7_8(value_2) == null) <!DEBUG_INFO_SMARTCAST!>value_2<!>.length
    }
}

fun case_8(value_1: Any?, value_2: Any?) {
    if (case_8_1(value_1)) {
        <!DEBUG_INFO_SMARTCAST!>value_1<!>?.toByte()
        if (case_8_2(<!DEBUG_INFO_SMARTCAST!>value_1<!>)) {
            <!DEBUG_INFO_SMARTCAST!>value_1<!>.toByte()
            if (case_8_3(<!DEBUG_INFO_SMARTCAST!>value_1<!>)) <!DEBUG_INFO_SMARTCAST!>value_1<!>.inv()
        }
    }
    if (!case_8_4(value_2)) {
        <!DEBUG_INFO_SMARTCAST!>value_2<!>?.toByte()
        if (!case_8_5(<!DEBUG_INFO_SMARTCAST!>value_2<!>)) {
            <!DEBUG_INFO_SMARTCAST!>value_2<!>.toByte()
            if (!case_8_6(<!DEBUG_INFO_SMARTCAST!>value_2<!>)) <!DEBUG_INFO_SMARTCAST!>value_2<!>.inv()
        }
    }
    if (case_8_7(value_2) != null) {
        <!DEBUG_INFO_SMARTCAST!>value_2<!>?.toByte()
        if (case_8_8(<!DEBUG_INFO_SMARTCAST!>value_2<!>) != null) {
            <!DEBUG_INFO_SMARTCAST!>value_2<!>.toByte()
            if (case_8_9(<!DEBUG_INFO_SMARTCAST!>value_2<!>) != null) <!DEBUG_INFO_SMARTCAST!>value_2<!>.inv()
        }
    }
    if (case_8_10(value_2) == null) {
        <!DEBUG_INFO_SMARTCAST!>value_2<!>?.toByte()
        if (case_8_11(<!DEBUG_INFO_SMARTCAST!>value_2<!>) == null) {
            <!DEBUG_INFO_SMARTCAST!>value_2<!>.toByte()
            if (case_8_12(<!DEBUG_INFO_SMARTCAST!>value_2<!>) == null) <!DEBUG_INFO_SMARTCAST!>value_2<!>.inv()
        }
    }
}

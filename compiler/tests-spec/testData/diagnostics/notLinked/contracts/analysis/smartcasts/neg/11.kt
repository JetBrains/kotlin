// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !WITH_CONTRACT_FUNCTIONS

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)

 SECTION: contracts
 CATEGORY: analysis, smartcasts
 NUMBER: 11
 DESCRIPTION: Check smartcasts using double negation (returnsFalse/invert type checking/not operator).
 UNEXPECTED BEHAVIOUR
 ISSUES: KT-26176
 */

// FILE: contracts.kt

package contracts

import kotlin.contracts.*

fun case_1(x: Any?): Boolean {
    contract { returns(true) implies (x !is Number) }
    return x !is Number
}

fun case_2(x: Any?): Boolean {
    contract { returns(true) implies (x !is Number?) }
    return x !is Number?
}

fun case_16_1(value_1: Any?, value_2: Any?): Boolean {
    contract { returns(true) implies (value_1 !is String || value_2 !is Number) }
    return value_1 !is String || value_2 !is Number
}
fun case_16_2(value_1: Any?, value_2: Any?): Boolean {
    contract { returns(false) implies (value_1 !is String || value_2 !is Number) }
    return !(value_1 !is String || value_2 !is Number)
}
fun case_16_3(value_1: Any?, value_2: Any?): Boolean? {
    contract { returnsNotNull() implies (value_1 !is String || value_2 !is Number) }
    return if (value_1 !is String || value_2 !is Number) true else null
}
fun case_16_4(value_1: Any?, value_2: Any?): Boolean? {
    contract { returns(null) implies (value_1 !is String || value_2 !is Number) }
    return if (value_1 !is String || value_2 !is Number) null else true
}

fun case_17_1(value_1: Any?, value_2: Any?): Boolean {
    contract { returns(true) implies (value_1 !is String || value_2 != null) }
    return value_1 !is String || value_2 != null
}
fun case_17_2(value_1: Any?, value_2: Any?): Boolean {
    contract { returns(false) implies (value_1 !is String || value_2 != null) }
    return !(value_1 !is String || value_2 != null)
}
fun case_17_3(value_1: Any?, value_2: Any?): Boolean? {
    contract { returnsNotNull() implies (value_1 !is String || value_2 != null) }
    return if (value_1 !is String || value_2 != null) true else null
}
fun case_17_4(value_1: Any?, value_2: Any?): Boolean? {
    contract { returns(null) implies (value_1 !is String || value_2 != null) }
    return if (value_1 !is String || value_2 != null) null else true
}

fun case_18_1(value_1: Any?, value_2: Any?, value_3: Any?, value_4: Any?): Boolean {
    contract { returns(true) implies (value_1 !is Float? || value_1 == null || value_2 == null || value_3 == null || value_4 == null) }
    return value_1 !is Float? || value_1 == null || value_2 == null || value_3 == null || value_4 == null
}
fun case_18_2(value_1: Any?, value_2: Any?, value_3: Any?, value_4: Any?): Boolean {
    contract { returns(false) implies (value_1 !is Float? || value_1 == null || value_2 == null || value_3 == null || value_4 == null) }
    return !(value_1 !is Float? || value_1 == null || value_2 == null || value_3 == null || value_4 == null)
}
fun case_18_3(value_1: Any?, value_2: Any?, value_3: Any?, value_4: Any?): Boolean? {
    contract { returnsNotNull() implies (value_1 !is Float? || value_1 == null || value_2 == null || value_3 == null || value_4 == null) }
    return if (value_1 !is Float? || value_1 == null || value_2 == null || value_3 == null || value_4 == null) true else null
}
fun case_18_4(value_1: Any?, value_2: Any?, value_3: Any?, value_4: Any?): Boolean? {
    contract { returns(null) implies (value_1 !is Float? || value_1 == null || value_2 == null || value_3 == null || value_4 == null) }
    return if (value_1 !is Float? || value_1 == null || value_2 == null || value_3 == null || value_4 == null) null else true
}

fun <T> T.case_19_1(): Boolean {
    contract { returns(true) implies (this@case_19_1 !is String) }
    return this@case_19_1 !is String
}
fun <T> T.case_19_2(): Boolean {
    contract { returns(false) implies (this@case_19_2 is String) }
    return !(this@case_19_2 is String)
}
fun <T> T.case_19_3(): Boolean? {
    contract { returnsNotNull() implies (this@case_19_3 is String) }
    return if (this@case_19_3 is String) true else null
}
fun <T> T.case_19_4(): Boolean? {
    contract { returns(null) implies (this@case_19_4 is String) }
    return if (this@case_19_4 is String) null else true
}

fun <T : Number> T.case_20_1(): Boolean {
    contract { returns(true) implies (this@case_20_1 !is Int) }
    return this@case_20_1 !is Int
}
fun <T : Number> T.case_20_2(): Boolean {
    contract { returns(false) implies (this@case_20_2 is Int) }
    return !(this@case_20_2 is Int)
}
fun <T : Number> T.case_20_3(): Boolean? {
    contract { returnsNotNull() implies (this@case_20_3 is Int) }
    return if (this@case_20_3 is Int) true else null
}
fun <T : Number> T.case_20_4(): Boolean? {
    contract { returns(null) implies (this@case_20_4 is Int) }
    return if (this@case_20_4 is Int) null else true
}

fun <T : <!FINAL_UPPER_BOUND!>String<!>> T?.case_21_1(): Boolean {
    contract { returns(true) implies (this@case_21_1 != null) }
    return this@case_21_1 != null
}
fun <T : <!FINAL_UPPER_BOUND!>String<!>> T?.case_21_2(): Boolean {
    contract { returns(true) implies (this@case_21_2 == null) }
    return this@case_21_2 == null
}
fun <T : <!FINAL_UPPER_BOUND!>String<!>> T?.case_21_3(): Boolean {
    contract { returns(false) implies (this@case_21_3 != null) }
    return !(this@case_21_3 != null)
}

fun <T : String?> T.case_22_1(): Boolean {
    contract { returns(true) implies (this@case_22_1 != null) }
    return this@case_22_1 != null
}
fun <T : String?> T.case_22_2(): Boolean {
    contract { returns(true) implies (this@case_22_2 == null) }
    return this@case_22_2 == null
}
fun <T : <!FINAL_UPPER_BOUND!>String<!>> T?.case_22_5(): Boolean? {
    contract { returnsNotNull() implies (this@case_22_5 != null) }
    return if (this@case_22_5 != null) true else null
}
fun <T : <!FINAL_UPPER_BOUND!>String<!>> T?.case_22_7(): Boolean? {
    contract { returns(null) implies (this@case_22_7 != null) }
    return if (this@case_22_7 != null) null else true
}

fun <T> T?.case_23_1(): Boolean {
    contract { returns(false) implies (this@case_23_1 == null || this@case_23_1 !is String) }
    return !(this@case_23_1 == null || this@case_23_1 !is String)
}
fun <T> T?.case_23_2(): Boolean? {
    contract { returnsNotNull() implies (this@case_23_2 == null || this@case_23_2 !is String) }
    return if (this@case_23_2 == null || this@case_23_2 !is String) true else null
}
fun <T> T?.case_23_3(): Boolean? {
    contract { returns(null) implies (this@case_23_3 == null || this@case_23_3 !is String) }
    return if (this@case_23_3 == null || this@case_23_3 !is String) null else true
}

fun <T : Number?> T.case_24_1(): Boolean {
    contract { returns(false) implies (this@case_24_1 !is Int || <!SENSELESS_COMPARISON!>this@case_24_1 == null<!>) }
    return !(this@case_24_1 !is Int || <!SENSELESS_COMPARISON!>this@case_24_1 == null<!>)
}
fun <T : Number?> T.case_24_2(): Boolean? {
    contract { returnsNotNull() implies (this@case_24_2 !is Int || <!SENSELESS_COMPARISON!>this@case_24_2 == null<!>) }
    return if (this@case_24_2 !is Int || <!SENSELESS_COMPARISON!>this@case_24_2 == null<!>) true else null
}
fun <T : Number?> T.case_24_3(): Boolean? {
    contract { returns(null) implies (this@case_24_3 !is Int || <!SENSELESS_COMPARISON!>this@case_24_3 == null<!>) }
    return if (this@case_24_3 !is Int || <!SENSELESS_COMPARISON!>this@case_24_3 == null<!>) null else true
}

inline fun <reified T : Any?> T?.case_25_1(): Boolean {
    contract { returns(false) implies (this@case_25_1 !is Number || this@case_25_1 !is Int || <!SENSELESS_COMPARISON!>this@case_25_1 == null<!>) }
    return !(this@case_25_1 !is Number || this@case_25_1 !is Int || <!SENSELESS_COMPARISON!>this@case_25_1 == null<!>)
}
inline fun <reified T : Any?> T?.case_25_2(): Boolean? {
    contract { returnsNotNull() implies (this@case_25_2 !is Number || this@case_25_2 !is Int || <!SENSELESS_COMPARISON!>this@case_25_2 == null<!>) }
    return if (this@case_25_2 !is Number || this@case_25_2 !is Int || <!SENSELESS_COMPARISON!>this@case_25_2 == null<!>) true else null
}
inline fun <reified T : Any?> T?.case_25_3(): Boolean? {
    contract { returns(null) implies (this@case_25_3 !is Number || this@case_25_3 !is Int || <!SENSELESS_COMPARISON!>this@case_25_3 == null<!>) }
    return if (this@case_25_3 !is Number || this@case_25_3 !is Int || <!SENSELESS_COMPARISON!>this@case_25_3 == null<!>) null else true
}

fun <T> T?.case_26_1(value_1: Int?): Boolean {
    contract { returns(false) implies (this@case_26_1 == null || this@case_26_1 !is String || value_1 == null) }
    return !(this@case_26_1 == null || this@case_26_1 !is String || value_1 == null)
}
fun <T> T?.case_26_2(value_1: Int?): Boolean? {
    contract { returnsNotNull() implies (this@case_26_2 == null || this@case_26_2 !is String || value_1 == null) }
    return if (this@case_26_2 == null || this@case_26_2 !is String || value_1 == null) true else null
}
fun <T> T?.case_26_3(value_1: Int?): Boolean? {
    contract { returns(null) implies (this@case_26_3 == null || this@case_26_3 !is String || value_1 == null) }
    return if (this@case_26_3 == null || this@case_26_3 !is String || value_1 == null) null else true
}

// FILE: usages.kt

import contracts.*

fun case_1(value_1: Any?) {
    if (!contracts.case_1(value_1)) println(value_1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>()) // no smartcast
}

fun case_2(value_1: Any?) {
    if (!contracts.case_2(value_1)) println(value_1?.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>()) // no smartcast
}

fun case_3(number: Int?) {
    if (!funWithReturnsTrueAndNullCheck(number)) number<!UNSAFE_CALL!>.<!>inc() // nullable receiver
}

fun case_5(value_1: Any?) {
    if (!funWithReturnsTrue(value_1 !is String)) println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
}

fun case_6(value_1: Any?) {
    if (!funWithReturnsTrueAndInvertCondition(value_1 is String)) println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
    if (funWithReturnsFalse(value_1 !is String)) println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
    if (funWithReturnsFalseAndInvertCondition(value_1 is String)) println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
    if (!(funWithReturnsNotNullAndInvertCondition(value_1 !is String) != null)) println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
    if (!(funWithReturnsNullAndInvertCondition(value_1 !is String) == null)) println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
}

fun case_7(value_1: Any?) {
    if (!funWithReturnsTrue(value_1 == null)) println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
}

fun case_8(value_1: Any?) {
    if (!funWithReturnsTrueAndInvertCondition(value_1 != null)) println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
    if (funWithReturnsFalse(value_1 == null)) println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
    if (funWithReturnsFalseAndInvertCondition(value_1 != null)) println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
}

fun case_9(value_1: Any?) {
    if (!funWithReturnsTrueAndInvertTypeCheck(value_1)) println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
    if (funWithReturnsFalseAndInvertTypeCheck(value_1)) println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
}

fun case_10(value_1: Number?) {
    if (!funWithReturnsTrueAndNullCheck(value_1)) println(value_1<!UNSAFE_CALL!>.<!>toByte())
    if (funWithReturnsFalseAndNullCheck(value_1)) println(value_1<!UNSAFE_CALL!>.<!>toByte())
    if (funWithReturnsFalseAndNotNullCheck(value_1)) println(value_1)
    if (!(funWithReturnsNotNullAndNullCheck(value_1) != null)) println(value_1)
    if (!(funWithReturnsNullAndNullCheck(value_1) == null)) println(value_1)
}

fun case_11(value_1: Any?, value_2: Any?) {
    if (!funWithReturnsTrueAndInvertCondition(value_1 is String && value_2 is Number)) {
        println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
        println(value_2.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
    }
}

fun case_12(value_1: Any?, value_2: Any?) {
    if (!funWithReturnsTrue(value_1 !is String || value_2 !is Number)) {
        println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
        println(value_2.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
    }
    if (funWithReturnsFalse(value_1 !is String || value_2 !is Number)) {
        println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
        println(value_2.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
    }
}

fun case_13(value_1: Any?, value_2: Any?) {
    if (!funWithReturnsTrue(value_1 !is String || value_2 != null)) {
        println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
        println(value_2?.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
    }
    if (funWithReturnsFalse(value_1 !is Float? || value_1 == null || value_2 == null)) {
        println(value_1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>dec<!>())
        println(value_2?.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
    }
    if (funWithReturnsNotNull(value_1 !is String || value_2 !is Number) == null) {
        println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
        println(value_2.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
    }
    if (funWithReturnsNull(value_1 !is String || value_2 !is Number) != null) {
        println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
        println(value_2.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
    }
}

fun case_14(value_1: Any?, value_2: Any?) {
    if (!funWithReturnsTrueAndInvertCondition(value_1 is Float? && value_1 != null && value_2 != null)) {
        println(value_1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>dec<!>())
        println(value_2?.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
    }
    if (funWithReturnsFalseAndInvertCondition(value_1 is String && value_2 is Number)) {
        println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
        println(value_2.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
    }
    if (funWithReturnsFalseAndInvertCondition(value_1 is String && value_2 == null)) {
        println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
        println(value_2?.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
    }
    if (funWithReturnsNotNullAndInvertCondition(value_1 is String && value_2 is Number) == null) {
        println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
        println(value_2.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
    }
    if (funWithReturnsNotNullAndInvertCondition(value_1 is String && value_2 == null) == null) {
        println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
        println(value_2?.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
    }
    if (funWithReturnsNotNull(value_1 is Float? && value_1 != null && value_2 != null) == null) {
        println(value_1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>dec<!>())
        println(value_2?.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
    }
    if (funWithReturnsNullAndInvertCondition(value_1 is String && value_2 is Number) != null) {
        println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
        println(value_2.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
    }
    if (funWithReturnsNullAndInvertCondition(value_1 is String && value_2 == null) != null) {
        println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
        println(value_2?.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
    }
    if (funWithReturnsNull(value_1 is Float? && value_1 != null && value_2 != null) != null) {
        println(value_1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>dec<!>())
        println(value_2?.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
    }
}

class case_15_class {
    val prop_1: Int? = 10

    fun case_15(value_1: Any?, value_2: Number?) {
        val o = case_15_class()
        if (!funWithReturnsTrueAndInvertCondition(value_1 is Float? && value_1 != null && value_2 != null && o.prop_1 != null)) {
            println(value_1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>dec<!>())
            println(value_2?.toByte())
            println(o.prop_1<!UNSAFE_CALL!>.<!>plus(3))
        }
        if (funWithReturnsFalse(value_1 !is Float? || value_1 == null || value_2 == null || o.prop_1 == null || this.prop_1 == null)) {
            println(value_1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>dec<!>())
            println(value_2?.toByte())
            println(o.prop_1<!UNSAFE_CALL!>.<!>plus(3))
        }
        if (funWithReturnsNotNull(value_1 !is Float? || value_1 == null || value_2 == null || o.prop_1 == null || this.prop_1 == null) == null) {
            println(value_1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>dec<!>())
            println(value_2?.toByte())
            println(o.prop_1<!UNSAFE_CALL!>.<!>plus(3))
        }
        if (funWithReturnsNull(value_1 !is Float? || value_1 == null || value_2 == null || o.prop_1 == null || this.prop_1 == null) != null) {
            println(value_1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>dec<!>())
            println(value_2?.toByte())
            println(o.prop_1<!UNSAFE_CALL!>.<!>plus(3))
        }
    }
}

fun case_16(value_1: Any?, value_2: Any?) {
    if (!contracts.case_16_1(value_1, value_2)) {
        println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
        println(value_2.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
    }
    if (contracts.case_16_2(value_1, value_2)) {
        println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
        println(value_2.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
    }
    if (!(contracts.case_16_3(value_1, value_2) != null)) {
        println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
        println(value_2.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
    }
    if (!(contracts.case_16_4(value_1, value_2) == null)) {
        println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
        println(value_2.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
    }
}

fun case_17(value_1: Any?, value_2: Any?) {
    if (!contracts.case_17_1(value_1, value_2)) {
        println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
        println(value_2?.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
    }
    if (contracts.case_17_2(value_1, value_2)) {
        println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
        println(value_2?.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
    }
    if (contracts.case_17_3(value_1, value_2) == null) {
        println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
        println(value_2?.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
    }
    if (contracts.case_17_4(value_1, value_2) != null) {
        println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
        println(value_2?.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
    }
}

class case_18_class {
    val prop_1: Int? = 10
    fun case_18(value_1: Any?, value_2: Number?) {
        val o = case_18_class()
        if (contracts.case_18_1(value_1, value_2, o.prop_1, this.prop_1)) {
            println(value_1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>dec<!>())
            println(value_2?.toByte())
            println(o.prop_1<!UNSAFE_CALL!>.<!>plus(3))
            println(this.prop_1<!UNSAFE_CALL!>.<!>plus(3))
        }
        if (contracts.case_18_2(value_1, value_2, o.prop_1, this.prop_1)) {
            println(value_1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>dec<!>())
            println(value_2?.toByte())
            println(o.prop_1<!UNSAFE_CALL!>.<!>plus(3))
            println(this.prop_1<!UNSAFE_CALL!>.<!>plus(3))
        }
        if (contracts.case_18_3(value_1, value_2, o.prop_1, this.prop_1) == null) {
            println(value_1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>dec<!>())
            println(value_2?.toByte())
            println(o.prop_1<!UNSAFE_CALL!>.<!>plus(3))
            println(this.prop_1<!UNSAFE_CALL!>.<!>plus(3))
        }
        if (contracts.case_18_4(value_1, value_2, o.prop_1, this.prop_1) != null) {
            println(value_1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>dec<!>())
            println(value_2?.toByte())
            println(o.prop_1<!UNSAFE_CALL!>.<!>plus(3))
            println(this.prop_1<!UNSAFE_CALL!>.<!>plus(3))
        }
    }
}

fun case_19(value_1: Any?) {
    if (!value_1.case_19_1()) println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
    if (value_1.case_19_2()) println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
    if (value_1.case_19_3() == null) println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
    if (value_1.case_19_4() != null) println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
}

fun case_20(value_1: Number) {
    when { !value_1.case_20_1() -> println(value_1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>inv<!>()) }
    when { value_1.case_20_2() -> println(value_1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>inv<!>()) }
    when { value_1.case_20_3() == null -> println(value_1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>inv<!>()) }
    when { value_1.case_20_4() != null -> println(value_1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>inv<!>()) }
}

fun case_21(value_1: String?, value_2: String?, value_3: String?, value_4: String?) {
    if (!value_1.case_21_1()) println(value_1)
    if (!value_2.case_21_2()) println(value_2<!UNSAFE_CALL!>.<!>length)
    when (value_3.case_21_3()) {
        true -> println(value_4<!UNSAFE_CALL!>.<!>length)
        false -> println(value_3)
    }
}

fun case_22(value_1: String?) {
    when { !value_1.case_22_1() -> println(value_1) }
    when { !value_1.case_22_2() -> println(value_1<!UNSAFE_CALL!>.<!>length) }
    when {
        value_1.case_22_5() == null ->  println(value_1<!UNSAFE_CALL!>.<!>length)
        value_1.case_22_5() != null ->  println(value_1)
    }
    when {
        value_1.case_22_7() != null ->  println(value_1<!UNSAFE_CALL!>.<!>length)
        value_1.case_22_7() == null ->  println(value_1)
    }
}

fun case_23(value_1: Any?, value_2: Any?) {
    when { value_1.case_23_1() -> println(value_1.<!UNRESOLVED_REFERENCE!>length<!>) }
    when { value_2.case_23_2() == null -> println(value_2.<!UNRESOLVED_REFERENCE!>length<!>) }
    when { value_2.case_23_3() != null -> println(value_2.<!UNRESOLVED_REFERENCE!>length<!>) }
}

fun case_24(value_1: Number?, value_2: Number?) {
    if (value_1.case_24_1()) println(value_1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>inv<!>())
    if (value_2.case_24_2() == null) println(value_2.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>inv<!>())
    if (value_2.case_24_3() != null) println(value_2.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>inv<!>())
}

fun case_25(value_1: Any?, value_2: Any?) {
    if (value_1.case_25_1()) println(value_1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>inv<!>())
    if (value_2.case_25_2() != null) println(value_2.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>inv<!>())
    if (value_2.case_25_3() == null) println(value_2.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>inv<!>())
}

fun case_26(value_1: Any?, value_2: Int?, value_3: Any?, value_4: Int?) {
    when {
        value_1.case_26_1(value_2) -> {
            println(value_1.<!UNRESOLVED_REFERENCE!>length<!>)
            println(value_2<!UNSAFE_CALL!>.<!>inv())
        }
    }
    when {
        value_3.case_26_2(value_4) == null -> {
            println(value_3.<!UNRESOLVED_REFERENCE!>length<!>)
            println(value_4<!UNSAFE_CALL!>.<!>inv())
        }
    }
    when {
        value_3.case_26_3(value_4) != null -> {
            println(value_3.<!UNRESOLVED_REFERENCE!>length<!>)
            println(value_4<!UNSAFE_CALL!>.<!>inv())
        }
    }
}

/*
 UNEXPECTED BEHAVIOUR
 */
fun case_27(value_1: Any?, value_2: Any?, value_3: Any?) {
    funWithReturnsAndInvertCondition(value_1 !is String? || value_2 !is Number && value_3 !is Float)
    println(<!DEBUG_INFO_SMARTCAST!>value_1<!>!!.<!UNRESOLVED_REFERENCE!>length<!>)
    println(value_2.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
    println(value_3.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>dec<!>())
}

/*
 UNEXPECTED BEHAVIOUR
 */
fun case_28(value_1: Any?, value_2: Any?, value_3: Any?) {
    funWithReturnsAndInvertCondition(value_1 !is String || value_2 !is Number || <!USELESS_IS_CHECK!>value_3 !is Any?<!>)
    println(<!DEBUG_INFO_SMARTCAST!>value_1<!><!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>.<!UNRESOLVED_REFERENCE!>length<!>)
    println(<!DEBUG_INFO_SMARTCAST!>value_2<!><!UNNECESSARY_SAFE_CALL!>?.<!>toByte())
    println(value_3.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>dec<!>())
}

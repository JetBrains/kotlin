// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)

 SECTION: contracts
 CATEGORY: analysis, smartcasts
 NUMBER: 5
 DESCRIPTION: Smartcasts using Returns effects with complex (conjunction/disjunction) type checking and not-null conditions on receiver inside contract.
 */

// FILE: contracts.kt

package contracts

import kotlin.contracts.*

fun <T> T?.case_1() {
    contract { returns() implies (this@case_1 != null && this@case_1 is String) }
    if (!(this@case_1 != null && this@case_1 is String)) throw Exception()
}

fun <T : Number?> T.case_2() {
    contract { returns() implies (this@case_2 is Int && <!SENSELESS_COMPARISON!>this@case_2 != null<!>) }
    if (!(this@case_2 is Int && <!SENSELESS_COMPARISON!>this@case_2 != null<!>)) throw Exception()
}

inline fun <reified T : Any?> T?.case_3() {
    contract { returns() implies (this@case_3 is Number && this@case_3 is Int && <!SENSELESS_COMPARISON!>this@case_3 != null<!>) }
    if (!(this@case_3 is Number && this@case_3 is Int && <!SENSELESS_COMPARISON!>this@case_3 != null<!>)) throw Exception()
}

fun <T> T?.case_4_1(): Boolean {
    contract { returns(true) implies (this@case_4_1 != null && this@case_4_1 is String) }
    return this@case_4_1 != null && this@case_4_1 is String
}
fun <T> T?.case_4_2(): Boolean {
    contract { returns(false) implies (this@case_4_2 != null && this@case_4_2 is String) }
    return !(this@case_4_2 != null && this@case_4_2 is String)
}
fun <T> T?.case_4_3(): Boolean? {
    contract { returnsNotNull() implies (this@case_4_3 != null && this@case_4_3 is String) }
    return if (this@case_4_3 != null && this@case_4_3 is String) true else null
}
fun <T> T?.case_4_4(): Boolean? {
    contract { returns(null) implies (this@case_4_4 != null && this@case_4_4 is String) }
    return if (this@case_4_4 != null && this@case_4_4 is String) null else true
}

fun <T : Number?> T.case_5_1(): Boolean {
    contract { returns(true) implies (this@case_5_1 is Int && <!SENSELESS_COMPARISON!>this@case_5_1 != null<!>) }
    return this@case_5_1 is Int && <!SENSELESS_COMPARISON!>this@case_5_1 != null<!>
}
fun <T : Number?> T.case_5_2(): Boolean {
    contract { returns(false) implies (this@case_5_2 is Int && <!SENSELESS_COMPARISON!>this@case_5_2 != null<!>) }
    return !(this@case_5_2 is Int && <!SENSELESS_COMPARISON!>this@case_5_2 != null<!>)
}
fun <T : Number?> T.case_5_3(): Boolean? {
    contract { returnsNotNull() implies (this@case_5_3 is Int && <!SENSELESS_COMPARISON!>this@case_5_3 != null<!>) }
    return if (this@case_5_3 is Int && <!SENSELESS_COMPARISON!>this@case_5_3 != null<!>) true else null
}
fun <T : Number?> T.case_5_4(): Boolean? {
    contract { returns(null) implies (this@case_5_4 is Int && <!SENSELESS_COMPARISON!>this@case_5_4 != null<!>) }
    return if (this@case_5_4 is Int && <!SENSELESS_COMPARISON!>this@case_5_4 != null<!>) null else true
}

inline fun <reified T : Any?> T?.case_6_1(): Boolean {
    contract { returns(true) implies (this@case_6_1 is Number && this@case_6_1 is Int && <!SENSELESS_COMPARISON!>this@case_6_1 != null<!>) }
    return this@case_6_1 is Number && this@case_6_1 is Int && <!SENSELESS_COMPARISON!>this@case_6_1 != null<!>
}
inline fun <reified T : Any?> T?.case_6_2(): Boolean {
    contract { returns(false) implies (this@case_6_2 is Number && this@case_6_2 is Int && <!SENSELESS_COMPARISON!>this@case_6_2 != null<!>) }
    return !(this@case_6_2 is Number && this@case_6_2 is Int && <!SENSELESS_COMPARISON!>this@case_6_2 != null<!>)
}
inline fun <reified T : Any?> T?.case_6_3(): Boolean? {
    contract { returnsNotNull() implies (this@case_6_3 is Number && this@case_6_3 is Int && <!SENSELESS_COMPARISON!>this@case_6_3 != null<!>) }
    return if (this@case_6_3 is Number && this@case_6_3 is Int && <!SENSELESS_COMPARISON!>this@case_6_3 != null<!>) true else null
}
inline fun <reified T : Any?> T?.case_6_4(): Boolean? {
    contract { returns(null) implies (this@case_6_4 is Number && this@case_6_4 is Int && <!SENSELESS_COMPARISON!>this@case_6_4 != null<!>) }
    return if (this@case_6_4 is Number && this@case_6_4 is Int && <!SENSELESS_COMPARISON!>this@case_6_4 != null<!>) null else true
}

// FILE: usages.kt

import contracts.*

fun case_1(value_1: Any?) {
    value_1.case_1()
    println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length)
}

fun case_2(value_1: Number?) {
    value_1.case_2()
    println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.inv())
}

fun case_3(value_1: Any?) {
    value_1.case_3()
    println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.inv())
}

fun case_4(value_1: Any?, value_2: Any?, value_3: Any?) {
    when { value_1.case_4_1() -> println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.length) }
    when { !value_2.case_4_2() -> println(<!DEBUG_INFO_SMARTCAST!>value_2<!>.length) }
    when { value_3.case_4_3() != null -> println(<!DEBUG_INFO_SMARTCAST!>value_3<!>.length) }
    when { value_3.case_4_4() == null -> println(<!DEBUG_INFO_SMARTCAST!>value_3<!>.length) }
}

fun case_5(value_1: Number?, value_2: Number?, value_3: Number?) {
    if (value_1.case_5_1()) println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.inv())
    if (!value_2.case_5_2()) println(<!DEBUG_INFO_SMARTCAST!>value_2<!>.inv())
    if (value_3.case_5_3() != null) println(<!DEBUG_INFO_SMARTCAST!>value_3<!>.inv())
    if (value_3.case_5_4() == null) println(<!DEBUG_INFO_SMARTCAST!>value_3<!>.inv())
}

fun case_6(value_1: Any?, value_2: Any?, value_3: Any?) {
    if (value_1.case_6_1()) println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.inv())
    if (!value_2.case_6_2()) println(<!DEBUG_INFO_SMARTCAST!>value_2<!>.inv())
    if (value_3.case_6_3() != null) println(<!DEBUG_INFO_SMARTCAST!>value_3<!>.inv())
    if (value_3.case_6_4() == null) println(<!DEBUG_INFO_SMARTCAST!>value_3<!>.inv())
}

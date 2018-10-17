// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !WITH_CONTRACT_FUNCTIONS

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)

 SECTION: contracts
 CATEGORIES: analysis, smartcasts
 NUMBER: 11
 DESCRIPTION: Check smartcasts with passing same fields of instances of the same class in contract function with conjunction not-null condition.
 ISSUES: KT-26300
 */

// FILE: contracts.kt

package contracts

import kotlin.contracts.*

fun case_3(value_1: Any?, value_2: Any?, value_3: Any?, value_4: Any?) {
    contract { returns() implies (value_1 is Float? && value_1 != null && value_2 != null && value_3 != null && value_4 != null) }
    if (!(value_1 is Float? && value_1 != null && value_2 != null && value_3 != null && value_4 != null)) throw Exception()
}

fun case_4(value_1: Any?, value_2: Any?, value_3: Any?, value_4: Any?): Boolean {
    contract { returns(true) implies (value_1 is Float? && value_1 != null && value_2 != null && value_3 != null && value_4 != null) }
    return value_1 is Float? && value_1 != null && value_2 != null && value_3 != null && value_4 != null
}

// FILE: usages.kt

import contracts.*

class case_1 {
    val prop_1: Int? = 10
    fun case_1(value_1: Any?, value_2: Number?) {
        val o = case_1()
        funWithReturns(value_1 is Float? && value_1 != null && value_2 != null && o.prop_1 != null && this.prop_1 != null)
        println(<!DEBUG_INFO_SMARTCAST!>o.prop_1<!>.plus(3))
        println(<!DEBUG_INFO_SMARTCAST!>this.prop_1<!>.plus(3))
    }
}

class case_2 {
    val prop_1: Int? = 10
    fun case_2(value_1: Any?, value_2: Number?) {
        val o = case_2()
        if (funWithReturnsTrue(value_1 is Float? && value_1 != null && value_2 != null && o.prop_1 != null && this.prop_1 != null)) {
            println(<!DEBUG_INFO_SMARTCAST!>o.prop_1<!>.plus(3))
            println(<!DEBUG_INFO_SMARTCAST!>this.prop_1<!>.plus(3))
        }
    }
}

class case_3 {
    val prop_1: Int? = 10
    fun case_3(value_1: Any?, value_2: Number?) {
        val o = case_3()
        contracts.case_3(value_1, value_2, o.prop_1, this.prop_1)
        println(<!DEBUG_INFO_SMARTCAST!>o.prop_1<!>.plus(3))
        println(<!DEBUG_INFO_SMARTCAST!>this.prop_1<!>.plus(3))
    }
}

class case_4 {
    val prop_1: Int? = 10
    fun case_4(value_1: Any?, value_2: Number?) {
        val o = case_4()
        if (contracts.case_4(value_1, value_2, o.prop_1, this.prop_1)) {
            println(<!DEBUG_INFO_SMARTCAST!>o.prop_1<!>.plus(3))
            println(<!DEBUG_INFO_SMARTCAST!>this.prop_1<!>.plus(3))
        }
    }
}

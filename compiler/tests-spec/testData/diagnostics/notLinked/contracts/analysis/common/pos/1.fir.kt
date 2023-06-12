// !OPT_IN: kotlin.contracts.ExperimentalContracts

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: contracts, analysis, common
 * NUMBER: 1
 * DESCRIPTION: Analysis by contracts with mixed CallsInPlace and Returns effects.
 */

// FILE: contracts.kt

package contracts

import kotlin.contracts.*

// TESTCASE NUMBER: 1
inline fun case_1(value_1: Int?, block: () -> Unit): Boolean {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        returns(true) implies (value_1 != null)
    }
    block()
    return value_1 != null
}

// TESTCASE NUMBER: 2
inline fun <T> T?.case_2(value_1: Int?, value_2: Any?, block: () -> Unit): Boolean? {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        returns(true) implies (value_1 != null && this@case_2 != null && value_2 is Boolean?)
        returns(false) implies (value_2 !is Boolean?)
        returns(null) implies ((value_1 == null || this@case_2 == null) && value_2 is Boolean?)
    }
    block()
    if (value_1 != null && this != null && value_2 is Boolean?) return true
    if (value_2 !is Boolean?) return false
    return null
}

// FILE: main.kt

import contracts.*

// TESTCASE NUMBER: 1
fun case_1(value_1: Int?) {
    val value_3: Int
    if (contracts.case_1(value_1) { value_3 = 10 }) {
        value_1.inv()
        println(value_3)
    } else {
        println(value_3)
    }
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Int?, value_2: Int?, value_3: Any?) {
    val value_4: Int
    when (value_1.case_2(value_2, value_3) { value_4 = 10 }) {
        true -> {
            println(value_3?.xor(true))
            println(value_4)
            println(value_1.inv())
            println(value_2.inv())
        }
        false -> {
            println(value_4)
            println(value_1)
            println(value_2)
        }
        null -> {
            println(value_3?.xor(true))
            println(value_4)
            println(value_1)
            println(value_2)
        }
    }
    println(value_4)
}

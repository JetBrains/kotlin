// !OPT_IN: kotlin.contracts.ExperimentalContracts

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
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
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
        returns(true) implies (value_1 != null)
    }

    block()

    return value_1 != null
}

// TESTCASE NUMBER: 2
inline fun <T> T?.case_2(value_1: Int?, value_2: Any?, block: () -> Unit): Boolean? {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
        returns(true) implies (value_1 == null && this@case_2 == null && value_2 !is Boolean?)
        returns(false) implies (value_2 is Boolean?)
        returns(null) implies ((value_1 != null || this@case_2 != null) && value_2 !is Boolean?)
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
    val value_2: Int
    if (contracts.case_1(value_1) { value_2 = 10 }) {
        println(<!UNINITIALIZED_VARIABLE!>value_2<!>)
    } else {
        value_1<!UNSAFE_CALL!>.<!>inv()
        println(<!UNINITIALIZED_VARIABLE!>value_2<!>)
    }
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Int?, value_2: Int?, value_3: Any?) {
    val value_4: Int
    when (value_1.case_2(value_2, value_3) { value_4 = 10 }) {
        true -> {
            <!OVERLOAD_RESOLUTION_AMBIGUITY!>println<!>(value_3<!UNNECESSARY_SAFE_CALL!>?.<!><!UNRESOLVED_REFERENCE!>xor<!>(true))
            println(<!UNINITIALIZED_VARIABLE!>value_4<!>)
            println(value_1<!UNSAFE_CALL!>.<!>inv())
            println(value_2<!UNSAFE_CALL!>.<!>inv())
        }
        false -> {
            println(<!UNINITIALIZED_VARIABLE!>value_4<!>)
            println(value_1)
            println(value_2)
        }
        null -> {
            <!OVERLOAD_RESOLUTION_AMBIGUITY!>println<!>(value_3<!UNNECESSARY_SAFE_CALL!>?.<!><!UNRESOLVED_REFERENCE!>xor<!>(true))
            println(<!UNINITIALIZED_VARIABLE!>value_4<!>)
            println(value_1)
            println(value_2)
        }
    }
}

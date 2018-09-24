// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect +UseCallsInPlaceEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)

 SECTION: contracts
 CATEGORY: analysis, common
 NUMBER: 1
 DESCRIPTION: Analysis by contracts with mixed CallsInPlace and Returns effects.
 */

// FILE: contracts.kt

package contracts

import kotlin.contracts.*

inline fun case_1(value_1: Int?, block: () -> Unit): Boolean {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
        returns(true) implies (value_1 != null)
    }

    block()

    return value_1 != null
}

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

// FILE: usages.kt

import contracts.*

fun case_1(value_1: Int?) {
    val value_2: Int
    if (contracts.case_1(value_1) { value_2 = 10 }) {
        println(<!UNINITIALIZED_VARIABLE!>value_2<!>)
    } else {
        value_1<!UNSAFE_CALL!>.<!>inv()
        println(value_2)
    }
}

fun case_2(value_1: Int?, value_2: Int?, value_3: Any?) {
    val value_4: Int
    when (value_1.case_2(value_2, value_3) { value_4 = 10 }) {
        true -> {
            println(value_3?.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>xor<!>(true))
            println(<!UNINITIALIZED_VARIABLE!>value_4<!>)
            println(<!DEBUG_INFO_CONSTANT!>value_1<!><!UNSAFE_CALL!>.<!>inv())
            println(<!DEBUG_INFO_CONSTANT!>value_2<!><!UNSAFE_CALL!>.<!>inv())
        }
        false -> {
            println(value_4)
            println(value_1)
            println(value_2)
        }
        null -> {
            println(value_3?.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>xor<!>(true))
            println(value_4)
            println(value_1)
            println(value_2)
        }
    }
}

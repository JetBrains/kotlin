// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !WITH_CONTRACT_FUNCTIONS
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)

 SECTION: contracts
 CATEGORY: analysis, smartcasts
 NUMBER: 8
 DESCRIPTION: Smartcasts using some Returns effects.
 */

// FILE: contracts.kt

package contracts

import kotlin.contracts.*

fun <T> T?.case_3(value_1: Int?, value_2: Boolean): Boolean {
    contract {
        returns(true) implies (value_1 != null)
        returns(false) implies (value_1 == null && !value_2)
        returns(null) implies (value_1 == null && value_2)
    }

    return value_1 == null
}

fun case_4(value_1: Number, block: (() -> Unit)?): Boolean? {
    contract {
        returns(true) implies (block != null)
        returns(false) implies (value_1 is Int)
        returns(null) implies (block == null)
    }

    return <!SENSELESS_COMPARISON!>value_1 == null<!>
}

fun String?.case_5(value_1: Number?): Boolean? {
    contract {
        returns(true) implies (value_1 == null)
        returns(false) implies (this@case_5 == null)
        returnsNotNull() implies (value_1 is Int)
    }

    return value_1 == null
}

fun <T> T?.case_6(value_1: Number, value_2: String?): Boolean? {
    contract {
        returns(true) implies (this@case_6 == null)
        returns(false) implies (value_1 is Int)
        returns(null) implies (this@case_6 is String)
        returnsNotNull() implies (value_2 == null)
    }

    return <!SENSELESS_COMPARISON!>value_1 == null<!>
}

// FILE: usages.kt

import contracts.*

fun case_1(value_1: Any?) {
    funWithReturns(value_1 !is Number?)
    println(value_1?.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
    if (funWithReturnsTrue(value_1 !is Number)) {
        println(value_1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
        if (funWithReturnsNotNull(value_1 is Int) == null) println(value_1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>inv<!>())
    }
}

fun case_2(value_1: Any?) {
    if (!funWithReturnsFalse(value_1 !is Number?)) {
        println(value_1?.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
        funWithReturns(value_1 !is Number)
        println(value_1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
        if (funWithReturnsNull(value_1 !is Int) == null) println(value_1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>inv<!>())
    }
}

fun case_3(value_1: Int?, value_2: Any?) {
    if (!value_1.case_3(value_1, value_2 is Number?)) {
        println(value_2?.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
        println(<!DEBUG_INFO_CONSTANT!>value_1<!>)
    } else if (value_1.case_3(value_1, value_2 is Number?)) {
        println(value_2?.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
    } else {
        println(value_2?.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toByte<!>())
    }
}

fun case_4(value_1: Number, value_2: (() -> Unit)?) {
    if (contracts.case_4(value_1, value_2) == true) {
        value_1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>inv<!>()
    } else if (contracts.case_4(value_1, value_2) == false) {
        println(value_2)
    } else if (contracts.case_4(value_1, value_2) == null) {
        <!UNSAFE_IMPLICIT_INVOKE_CALL, DEBUG_INFO_CONSTANT!>value_2<!>()
    }
}

fun case_5(value_1: Number?, value_2: String?) {
    when (value_2.case_5(value_1)) {
        true -> {
            println(value_2<!UNSAFE_CALL!>.<!>length)
            println(<!DEBUG_INFO_CONSTANT!>value_1<!><!UNSAFE_CALL!>.<!>toByte())
        }
        false -> {
            println(<!DEBUG_INFO_CONSTANT!>value_2<!><!UNSAFE_CALL!>.<!>length)
            println(value_1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>inv<!>())
        }
    }
}

fun case_6(value_1: Number, value_2: String?, value_3: Any?) {
    when (value_3.case_6(value_1, value_2)) {
        true -> {
            println(<!DEBUG_INFO_CONSTANT!>value_3<!>.equals(""))
            println(value_2<!UNSAFE_CALL!>.<!>length)
        }
        false -> {
            println(value_3.<!UNRESOLVED_REFERENCE!>length<!>)
            println(value_2<!UNSAFE_CALL!>.<!>length)
        }
        null -> {
            println(value_1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>inv<!>())
        }
    }
}

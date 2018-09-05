// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !WITH_CONTRACT_FUNCTIONS
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)

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
        returns(true) implies (value_1 == null)
        returns(false) implies (value_1 != null && value_2)
        returns(null) implies (value_1 != null && !value_2)
    }

    return value_1 == null
}

fun case_4(value_1: Number, block: (() -> Unit)?): Boolean? {
    contract {
        returns(true) implies (value_1 is Int)
        returns(false) implies (block == null)
        returns(null) implies (block != null)
    }

    return <!SENSELESS_COMPARISON!>value_1 == null<!>
}

fun String?.case_5(value_1: Number?): Boolean? {
    contract {
        returns(true) implies (value_1 != null)
        returns(false) implies (value_1 is Int)
        returnsNotNull() implies (this@case_5 != null)
    }

    return value_1 == null
}

fun <T> T?.case_6(value_1: Number, value_2: String?): Boolean? {
    contract {
        returns(true) implies (this@case_6 != null)
        returns(false) implies (this@case_6 is String)
        returns(null) implies (value_1 is Int)
        returnsNotNull() implies (value_2 != null)
    }

    return <!SENSELESS_COMPARISON!>value_1 == null<!>
}

// FILE: usages.kt

import contracts.*

fun case_1(value_1: Any?) {
    funWithReturns(value_1 is Number?)
    println(<!DEBUG_INFO_SMARTCAST!>value_1<!>?.toByte())
    if (funWithReturnsTrue(value_1 is Number)) {
        println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.toByte())
        if (funWithReturnsNotNull(value_1 is Int) != null) println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.inv())
    }
}

fun case_2(value_1: Any?) {
    if (!funWithReturnsFalse(value_1 is Number?)) {
        println(<!DEBUG_INFO_SMARTCAST!>value_1<!>?.toByte())
        funWithReturns(value_1 is Number)
        println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.toByte())
        if (funWithReturnsNull(value_1 is Int) == null) println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.inv())
    }
}

fun case_3(value_1: Int?, value_2: Any?) {
    if (!value_1.case_3(value_1, value_2 is Number?)) {
        println(<!DEBUG_INFO_SMARTCAST!>value_2<!>?.toByte())
        println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.inv())
    } else if (<!DEBUG_INFO_CONSTANT!>value_1<!>.case_3(<!DEBUG_INFO_CONSTANT!>value_1<!>, value_2 is Number?)) {
        println(<!DEBUG_INFO_CONSTANT!>value_1<!>)
    } else {
        <!UNREACHABLE_CODE!>println(<!><!DEBUG_INFO_SMARTCAST!>value_1<!><!UNREACHABLE_CODE!>.inv())<!>
    }
}

fun case_4(value_1: Number, value_2: (() -> Unit)?) {
    if (contracts.case_4(value_1, value_2) == true) {
        <!DEBUG_INFO_SMARTCAST!>value_1<!>.inv()
    } else if (contracts.case_4(value_1, value_2) == false) {
        println(<!DEBUG_INFO_CONSTANT!>value_2<!>)
    } else if (contracts.case_4(value_1, value_2) == null) {
        <!DEBUG_INFO_SMARTCAST!>value_2<!>()
    }
}

/*
 UNEXPECTED BEHAVIOUR: unsafe calls
 ISSUES: KT-26612
 */
fun case_5(value_1: Number?, value_2: String?) {
    when (value_2.case_5(value_1)) {
        true -> {
            println(value_2<!UNSAFE_CALL!>.<!>length)
            println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.toByte())
        }
        false -> {
            println(value_2<!UNSAFE_CALL!>.<!>length)
            println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.inv())
        }
    }
}

/*
 UNEXPECTED BEHAVIOUR: unsafe calls
 ISSUES: KT-26612
 */
fun case_6(value_1: Number, value_2: String?, value_3: Any?) {
    when (value_3.case_6(value_1, value_2)) {
        true -> {
            println(<!DEBUG_INFO_SMARTCAST!>value_3<!>.equals(""))
            println(value_2<!UNSAFE_CALL!>.<!>length)
        }
        false -> {
            println(<!DEBUG_INFO_SMARTCAST!>value_3<!>.length)
            println(value_2<!UNSAFE_CALL!>.<!>length)
        }
        null -> {
            println(<!DEBUG_INFO_SMARTCAST!>value_1<!>.inv())
        }
    }
}

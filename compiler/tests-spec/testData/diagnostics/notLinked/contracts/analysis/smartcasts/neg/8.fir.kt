// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !WITH_NEW_INFERENCE

// FILE: contracts.kt

package contracts

import kotlin.contracts.*

// TESTCASE NUMBER: 3
fun <T> T?.case_3(value_1: Int?, value_2: Boolean): Boolean {
    <!WRONG_IMPLIES_CONDITION!>contract {
        returns(true) implies (value_1 != null)
        returns(false) implies (value_1 == null && !value_2)
        returns(null) implies (value_1 == null && value_2)
    }<!>

    return value_1 == null
}

// TESTCASE NUMBER: 4
fun case_4(value_1: Number, block: (() -> Unit)?): Boolean? {
    <!WRONG_IMPLIES_CONDITION, WRONG_IMPLIES_CONDITION!>contract {
        returns(true) implies (block != null)
        returns(false) implies (value_1 is Int)
        returns(null) implies (block == null)
    }<!>

    return value_1 == null
}

// TESTCASE NUMBER: 5
fun String?.case_5(value_1: Number?): Boolean? {
    <!WRONG_IMPLIES_CONDITION!>contract {
        returns(true) implies (value_1 == null)
        returns(false) implies (this@case_5 == null)
        returnsNotNull() implies (value_1 is Int)
    }<!>

    return value_1 == null
}

// TESTCASE NUMBER: 6
fun <T> T?.case_6(value_1: Number, value_2: String?): Boolean? {
    <!WRONG_IMPLIES_CONDITION!>contract {
        returns(true) implies (this@case_6 == null)
        returns(false) implies (value_1 is Int)
        returns(null) implies (this@case_6 is String)
        returnsNotNull() implies (value_2 == null)
    }<!>

    return value_1 == null
}

// FILE: main.kt

import contracts.*

// TESTCASE NUMBER: 1
fun case_1(value_1: Any?) {
    funWithReturns(value_1 !is Number?)
    <!AMBIGUITY!>println<!>(value_1?.<!UNRESOLVED_REFERENCE!>toByte<!>())
    if (funWithReturnsTrue(value_1 !is Number)) {
        <!AMBIGUITY!>println<!>(value_1.<!UNRESOLVED_REFERENCE!>toByte<!>())
        if (funWithReturnsNotNull(value_1 is Int) == null) <!AMBIGUITY!>println<!>(value_1.inv())
    }
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Any?) {
    if (!funWithReturnsFalse(value_1 !is Number?)) {
        <!AMBIGUITY!>println<!>(value_1?.<!UNRESOLVED_REFERENCE!>toByte<!>())
        funWithReturns(value_1 !is Number)
        <!AMBIGUITY!>println<!>(value_1.<!UNRESOLVED_REFERENCE!>toByte<!>())
        if (funWithReturnsNull(value_1 !is Int) == null) <!AMBIGUITY!>println<!>(value_1.inv())
    }
}

// TESTCASE NUMBER: 3
fun case_3(value_1: Int?, value_2: Any?) {
    if (!value_1.case_3(value_1, value_2 is Number?)) {
        <!AMBIGUITY!>println<!>(value_2?.<!UNRESOLVED_REFERENCE!>toByte<!>())
        println(value_1)
    } else if (value_1.case_3(value_1, value_2 is Number?)) {
        <!AMBIGUITY!>println<!>(value_2?.<!UNRESOLVED_REFERENCE!>toByte<!>())
    } else {
        <!AMBIGUITY!>println<!>(value_2?.<!UNRESOLVED_REFERENCE!>toByte<!>())
    }
}

// TESTCASE NUMBER: 4
fun case_4(value_1: Number, value_2: (() -> Unit)?) {
    if (contracts.case_4(value_1, value_2) == true) {
        value_1.inv()
    } else if (contracts.case_4(value_1, value_2) == false) {
        println(value_2)
    } else if (contracts.case_4(value_1, value_2) == null) {
        value_2()
    }
}

// TESTCASE NUMBER: 5
fun case_5(value_1: Number?, value_2: String?) {
    when (value_2.case_5(value_1)) {
        true -> {
            println(value_2.<!INAPPLICABLE_CANDIDATE!>length<!>)
            println(value_1.toByte())
        }
        false -> {
            println(value_2.<!INAPPLICABLE_CANDIDATE!>length<!>)
            <!AMBIGUITY!>println<!>(value_1.inv())
        }
    }
}

// TESTCASE NUMBER: 6
fun case_6(value_1: Number, value_2: String?, value_3: Any?) {
    when (value_3.case_6(value_1, value_2)) {
        true -> {
            println(value_3.equals(""))
            println(value_2.<!INAPPLICABLE_CANDIDATE!>length<!>)
        }
        false -> {
            <!AMBIGUITY!>println<!>(value_3.<!UNRESOLVED_REFERENCE!>length<!>)
            println(value_2.<!INAPPLICABLE_CANDIDATE!>length<!>)
        }
        null -> {
            println(value_1.inv())
        }
    }
}

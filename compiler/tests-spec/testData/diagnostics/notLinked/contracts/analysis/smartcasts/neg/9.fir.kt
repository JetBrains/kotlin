// !OPT_IN: kotlin.contracts.ExperimentalContracts
// SKIP_TXT

// TESTCASE NUMBER: 1
fun case_1(arg: Int?) {
    funWithAtMostOnceCallsInPlace { arg!! }
    arg<!UNSAFE_CALL!>.<!>inc()
}

// TESTCASE NUMBER: 2
fun case_2(arg: Int?) {
    funWithUnknownCallsInPlace { arg!! }
    arg<!UNSAFE_CALL!>.<!>inc()
}

// TESTCASE NUMBER: 3
fun case_3() {
    val value_1: Boolean?
    funWithAtMostOnceCallsInPlace { value_1 = false }
    <!UNINITIALIZED_VARIABLE!>value_1<!><!UNSAFE_CALL!>.<!>not()
}

// TESTCASE NUMBER: 4
fun case_4() {
    val value_1: Boolean?
    funWithUnknownCallsInPlace { <!VAL_REASSIGNMENT!>value_1<!> = true }
    <!UNINITIALIZED_VARIABLE!>value_1<!><!UNSAFE_CALL!>.<!>not()
}

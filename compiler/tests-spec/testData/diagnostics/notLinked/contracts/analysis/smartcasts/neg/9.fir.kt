// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// SKIP_TXT

// TESTCASE NUMBER: 1
fun case_1(arg: Int?) {
    funWithAtMostOnceCallsInPlace { arg!! }
    arg.<!INAPPLICABLE_CANDIDATE!>inc<!>()
}

// TESTCASE NUMBER: 2
fun case_2(arg: Int?) {
    funWithUnknownCallsInPlace { arg!! }
    arg.<!INAPPLICABLE_CANDIDATE!>inc<!>()
}

// TESTCASE NUMBER: 3
fun case_3() {
    val value_1: Boolean?
    funWithAtMostOnceCallsInPlace { value_1 = false }
    <!UNINITIALIZED_VARIABLE!>value_1<!>.<!INAPPLICABLE_CANDIDATE!>not<!>()
}

// TESTCASE NUMBER: 4
fun case_4() {
    val value_1: Boolean?
    funWithUnknownCallsInPlace { value_1 = true }
    <!UNINITIALIZED_VARIABLE!>value_1<!>.<!INAPPLICABLE_CANDIDATE!>not<!>()
}

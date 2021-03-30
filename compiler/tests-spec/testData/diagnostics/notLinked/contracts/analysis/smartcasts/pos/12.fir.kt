// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// SKIP_TXT

// TESTCASE NUMBER: 1
fun case_1(arg: Int?) {
    funWithExactlyOnceCallsInPlace { arg!! }
    arg.inc()
}

// TESTCASE NUMBER: 2
fun case_2(arg: Int?) {
    funWithAtLeastOnceCallsInPlace { arg!! }
    arg<!UNSAFE_CALL!>.<!>inc()
}

// TESTCASE NUMBER: 3
fun case_3() {
    val value_1: Boolean?
    funWithExactlyOnceCallsInPlace { value_1 = false }
    value_1.not()
}

// TESTCASE NUMBER: 4
fun case_4() {
    val value_1: Boolean?
    funWithAtLeastOnceCallsInPlace { <!VAL_REASSIGNMENT!>value_1<!> = true }
    value_1<!UNSAFE_CALL!>.<!>not()
}

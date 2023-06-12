// FIR_IDENTICAL
// !OPT_IN: kotlin.contracts.ExperimentalContracts
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: contracts, analysis, smartcasts
 * NUMBER: 9
 * DESCRIPTION: Check the lack of smartcasts after non-null assertions or not-null value assignment in lambdas of contract function with 'unknown' or 'at most once' CallsInPlace effects.
 * HELPERS: contractFunctions
 */

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

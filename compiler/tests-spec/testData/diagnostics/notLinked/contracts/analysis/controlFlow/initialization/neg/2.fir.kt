// !OPT_IN: kotlin.contracts.ExperimentalContracts
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: contracts, analysis, controlFlow, initialization
 * NUMBER: 2
 * DESCRIPTION: val/var reassignment and/or uninitialized variable usages based on nested CallsInPlace effects with wrong invocation kind
 * HELPERS: contractFunctions
 */

// TESTCASE NUMBER: 1
fun case_1() {
    val value_1: Int
    funWithAtLeastOnceCallsInPlace {
        funWithAtMostOnceCallsInPlace {
            <!VAL_REASSIGNMENT!>value_1<!> = 1
            funWithExactlyOnceCallsInPlace {
                value_1.inc()
            }
        }
        funWithExactlyOnceCallsInPlace {
            <!UNINITIALIZED_VARIABLE!>value_1<!>.inc()
        }
        <!UNINITIALIZED_VARIABLE!>value_1<!>.inc()
    }
    <!UNINITIALIZED_VARIABLE!>value_1<!>.inc()
}

// TESTCASE NUMBER: 2
fun case_2() {
    val value_1: Int
    funWithAtMostOnceCallsInPlace {
        funWithAtMostOnceCallsInPlace {
            value_1 = 1
        }
        funWithAtLeastOnceCallsInPlace {
            <!UNINITIALIZED_VARIABLE!>value_1<!>.inc()
        }
        funWithUnknownCallsInPlace {
            <!UNINITIALIZED_VARIABLE!>value_1<!>.inc()
        }
        <!UNINITIALIZED_VARIABLE!>value_1<!>.inc()
    }
    <!UNINITIALIZED_VARIABLE!>value_1<!>.inc()
}

// TESTCASE NUMBER: 3
fun case_3() {
    var value_1: Int
    funWithAtLeastOnceCallsInPlace {
        funWithAtMostOnceCallsInPlace {
            value_1 = 1
            funWithExactlyOnceCallsInPlace {
                value_1.inc()
            }
        }
        funWithExactlyOnceCallsInPlace {
            <!UNINITIALIZED_VARIABLE!>value_1<!>.inc()
        }
        <!UNINITIALIZED_VARIABLE!>value_1<!>.inc()
    }
    <!UNINITIALIZED_VARIABLE!>value_1<!>.inc()
}

// TESTCASE NUMBER: 4
fun case_4() {
    var value_1: Int
    funWithAtLeastOnceCallsInPlace {
        funWithUnknownCallsInPlace {
            value_1 = 1
        }
        funWithAtLeastOnceCallsInPlace {
            <!UNINITIALIZED_VARIABLE!>value_1<!>.inc()
        }
        funWithUnknownCallsInPlace {
            <!UNINITIALIZED_VARIABLE!>value_1<!>.inc()
        }
        funWithExactlyOnceCallsInPlace {
            <!UNINITIALIZED_VARIABLE!>value_1<!>.inc()
        }
        <!UNINITIALIZED_VARIABLE!>value_1<!>.inc()
    }
    <!UNINITIALIZED_VARIABLE!>value_1<!>.inc()
}

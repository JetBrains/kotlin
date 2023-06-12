// !OPT_IN: kotlin.contracts.ExperimentalContracts
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: contracts, analysis, controlFlow, initialization
 * NUMBER: 4
 * DESCRIPTION: CallsInPlace contract functions with name shadowing and wrong invocation kind
 * HELPERS: contractFunctions
 */

// TESTCASE NUMBER: 1
fun case_1() {
    val value_1: Int
    funWithExactlyOnceCallsInPlace {
        val value_1 = 10
        value_1.inc()
    }
    <!UNINITIALIZED_VARIABLE!>value_1<!>.inc()
}

// TESTCASE NUMBER: 2
fun case_2() {
    val value_1: Int
    funWithExactlyOnceCallsInPlace {
        val value_1: Int
        funWithExactlyOnceCallsInPlace {
            value_1 = 10
        }
        funWithAtLeastOnceCallsInPlace {
            value_1.inc()
        }
        value_1.inc()
    }
    <!UNINITIALIZED_VARIABLE!>value_1<!>.inc()
}

// TESTCASE NUMBER: 3
fun case_3() {
    val value_1: Int
    funWithAtLeastOnceCallsInPlace {
        val value_1: Int
        funWithAtMostOnceCallsInPlace {
            value_1 = 10
        }
        funWithAtMostOnceCallsInPlace {
            <!UNINITIALIZED_VARIABLE!>value_1<!>.inc()
        }
        <!UNINITIALIZED_VARIABLE!>value_1<!>.inc()
    }
    funWithAtMostOnceCallsInPlace {
        value_1 = 10
    }
    <!UNINITIALIZED_VARIABLE!>value_1<!>.inc()
}

// TESTCASE NUMBER: 4
fun case_4() {
    var value_1: Int
    funWithAtLeastOnceCallsInPlace {
        val value_1: Int
        funWithExactlyOnceCallsInPlace {
            value_1 = 10
        }
        funWithAtMostOnceCallsInPlace {
            value_1.inc()
        }
        value_1.inc()
    }
    funWithAtMostOnceCallsInPlace {
        value_1 = 1
    }
    <!UNINITIALIZED_VARIABLE!>value_1<!>.dec()
}

// TESTCASE NUMBER: 5
fun case_5() {
    val value_1: Int
    funWithUnknownCallsInPlace {
        var value_1: Int
        funWithAtLeastOnceCallsInPlace {
            value_1 = 10
        }
        funWithUnknownCallsInPlace {
            value_1.inc()
        }
        value_1.inc()
    }
    funWithUnknownCallsInPlace {
        <!VAL_REASSIGNMENT!>value_1<!> = 1
    }
    <!UNINITIALIZED_VARIABLE!>value_1<!>.dec()
}

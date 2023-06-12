// !OPT_IN: kotlin.contracts.ExperimentalContracts
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: contracts, analysis, controlFlow, initialization
 * NUMBER: 4
 * DESCRIPTION: CallsInPlace contract functions with name shadowing
 * HELPERS: contractFunctions
 */

// TESTCASE NUMBER: 1
fun case_1() {
    val value_1: Int
    funWithExactlyOnceCallsInPlace {
        val value_1 = 10
        value_1.inc()
    }
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
}

// TESTCASE NUMBER: 3
fun case_3() {
    val value_1: Int
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
    funWithExactlyOnceCallsInPlace {
        value_1 = 10
    }
    value_1.inc()
}

// TESTCASE NUMBER: 4
fun case_4() {
    val value_1: Int
    funWithAtMostOnceCallsInPlace {
        val value_1: Int
        funWithExactlyOnceCallsInPlace {
            value_1 = 10
        }
        funWithUnknownCallsInPlace {
            value_1.inc()
        }
        value_1.inc()
    }
    funWithExactlyOnceCallsInPlace {
        value_1 = 10
    }
    value_1.inc()
}

// TESTCASE NUMBER: 5
fun case_5() {
    val value_1: Int
    funWithUnknownCallsInPlace {
        val value_1: Int
        funWithExactlyOnceCallsInPlace {
            value_1 = 10
        }
        funWithAtMostOnceCallsInPlace {
            value_1.inc()
        }
    }
    funWithExactlyOnceCallsInPlace {
        value_1 = 10
    }
    value_1.inc()
}

// TESTCASE NUMBER: 6
fun case_6() {
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
    funWithAtLeastOnceCallsInPlace { value_1 = 1 }
    value_1.dec()
}

// TESTCASE NUMBER: 7
fun case_7() {
    val value_1: Int
    funWithAtLeastOnceCallsInPlace {
        var value_1: Int
        funWithAtLeastOnceCallsInPlace { value_1 = 10 }
        funWithUnknownCallsInPlace { value_1.inc() }
        value_1.inc()
    }
    funWithExactlyOnceCallsInPlace { value_1 = 1 }
    value_1.dec()
}

// TESTCASE NUMBER: 8
fun case_8() {
    var value_1: Int
    funWithAtLeastOnceCallsInPlace {
        var value_1: Int
        funWithAtLeastOnceCallsInPlace {
            value_1 = 10
        }
        funWithAtLeastOnceCallsInPlace {
            value_1.inc()
        }
        value_1++
    }
    funWithAtLeastOnceCallsInPlace {
        value_1 = 1
    }
    value_1--
}

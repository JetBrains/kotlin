// IGNORE_REVERSED_RESOLVE
// FIR_IDENTICAL
// !OPT_IN: kotlin.contracts.ExperimentalContracts
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: contracts, analysis, controlFlow, initialization
 * NUMBER: 1
 * DESCRIPTION: val/var assignments using contract functions with CallsInPlace effect
 * HELPERS: contractFunctions
 */

// TESTCASE NUMBER: 1
fun case_1() {
    val value_1: Int
    funWithExactlyOnceCallsInPlace { value_1 = 10 }
    value_1.inc()
}

// TESTCASE NUMBER: 2
fun case_2() {
    var value_1: Int
    var value_2: Int
    funWithExactlyOnceCallsInPlace { value_1 = 10 }
    funWithAtLeastOnceCallsInPlace { value_2 = 10 }
    value_1.dec()
    value_2.div(10)
}

// TESTCASE NUMBER: 3
class case_3 {
    val value_1: Int
    var value_2: Int
    var value_3: Int
    init {
        funWithExactlyOnceCallsInPlace { value_1 = 1 }
        funWithExactlyOnceCallsInPlace { value_2 = 2 }
        funWithAtLeastOnceCallsInPlace { value_3 = 3 }
    }
}

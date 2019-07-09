// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: contracts, analysis, common
 * NUMBER: 2
 * DESCRIPTION: Recursion in the lambda of contract function.
 * HELPERS: contractFunctions
 */

// TESTCASE NUMBER: 1
fun case_1(x: Int): Unit = funWithExactlyOnceCallsInPlace {
    if (x == 0) return
    if (x == 1) return
    return case_1(x - 2)
}

// TESTCASE NUMBER: 2
fun case_2(x: Int) {
    funWithAtLeastOnceCallsInPlace {
        if (x == 0) return
        if (x == 1) return
        return case_2(x - 2)
    }
}

// TESTCASE NUMBER: 3
fun case_3(x: Int) {
    funWithAtMostOnceCallsInPlace {
        if (x == 0) return
        if (x == 1) return
        return case_3(x - 2)
    }
}

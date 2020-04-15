// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: contracts, analysis, controlFlow, unreachableCode
 * NUMBER: 1
 * DESCRIPTION: Using not allowed break and continue inside lambda of contract function
 * HELPERS: contractFunctions
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: Boolean) {
    while (value_1) {
        funWithExactlyOnceCallsInPlace {
            break
        }
        println("1")
    }

    loop@ for (i in 0..10) {
        funWithExactlyOnceCallsInPlace {
            break@loop
        }
        println("1")
    }
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Boolean) {
    for (i in 0..10) {
        funWithExactlyOnceCallsInPlace {
            continue
        }
        println("1")
    }

    loop@ while (value_1) {
        funWithExactlyOnceCallsInPlace {
            continue@loop
        }
        println("1")
    }
}

// !OPT_IN: kotlin.contracts.ExperimentalContracts
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: contracts, analysis, controlFlow, unreachableCode
 * NUMBER: 4
 * DESCRIPTION: Unreachable code detection using nested contract functions with CallsInPlace effect
 * HELPERS: contractFunctions
 */

// TESTCASE NUMBER: 1
fun case_1() {
    funWithExactlyOnceCallsInPlace {
        funWithExactlyOnceCallsInPlace {
            funWithExactlyOnceCallsInPlace {
                throw Exception()
            }
            println("1")
        }
        println("2")
    }
    println("3")
}

// TESTCASE NUMBER: 2
fun case_2() {
    funWithAtLeastOnceCallsInPlace {
        funWithAtLeastOnceCallsInPlace label_1@ {
            funWithAtLeastOnceCallsInPlace {
                return@label_1
            }
            println("1")
        }
        println("2")
    }
    funWithAtLeastOnceCallsInPlace {
        return
    }
    println("3")
}

// TESTCASE NUMBER: 3
fun case_3() {
    funWithExactlyOnceCallsInPlace {
        funWithExactlyOnceCallsInPlace {
            funWithExactlyOnceCallsInPlace {
                return@funWithExactlyOnceCallsInPlace
            }
            println("1")
        }
        println("2")
    }
    println("3")
}

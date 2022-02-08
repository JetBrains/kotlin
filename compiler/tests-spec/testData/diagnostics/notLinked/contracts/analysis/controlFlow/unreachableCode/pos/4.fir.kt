// !OPT_IN: kotlin.contracts.ExperimentalContracts
// SKIP_TXT

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

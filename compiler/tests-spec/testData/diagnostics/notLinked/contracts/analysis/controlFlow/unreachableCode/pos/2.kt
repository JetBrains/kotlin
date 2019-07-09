// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: contracts, analysis, controlFlow, unreachableCode
 * NUMBER: 2
 * DESCRIPTION: Check for lack of unreachable code report when 'at most once' and 'unknown' invokations in CallsInPlace effect used.
 * HELPERS: contractFunctions
 */

// TESTCASE NUMBER: 1
fun case_1() {
    funWithAtMostOnceCallsInPlace {
        throw Exception()
    }
    funWithAtMostOnceCallsInPlace {
        return
    }
    println("1")
}

// TESTCASE NUMBER: 2
fun case_2() {
    funWithUnknownCallsInPlace {
        throw Exception()
    }
    funWithUnknownCallsInPlace {
        return
    }
    println("1")
}

// TESTCASE NUMBER: 3
fun case_3() {
    funWithExactlyOnceCallsInPlace {
        return@funWithExactlyOnceCallsInPlace
    }
    println("1")
    funWithExactlyOnceCallsInPlace {
        fun nestedFun_1() {
            return@nestedFun_1
        }
    }
    println("1")
    fun nestedFun_3() {
        fun nestedFun_4() {
            funWithExactlyOnceCallsInPlace {
                return@nestedFun_4
            }
        }
        println("1")
    }
    println("1")
}

// TESTCASE NUMBER: 4
fun case_4() {
    funWithAtLeastOnceCallsInPlace {
        return@funWithAtLeastOnceCallsInPlace
    }
    println("1")
    funWithAtLeastOnceCallsInPlace {
        fun nestedFun_1() {
            return@nestedFun_1
        }
    }
    println("1")
    fun nestedFun_2() {
        fun nestedFun_3() {
            funWithAtLeastOnceCallsInPlace {
                return@nestedFun_3
            }
        }
        println("1")
    }
    println("1")
}

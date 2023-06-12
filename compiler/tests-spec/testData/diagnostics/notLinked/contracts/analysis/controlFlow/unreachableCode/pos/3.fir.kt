// !OPT_IN: kotlin.contracts.ExperimentalContracts
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: contracts, analysis, controlFlow, unreachableCode
 * NUMBER: 3
 * DESCRIPTION: Unreachable code detection using local functions or labdas combined with contract functions with CallsInPlace effect
 * HELPERS: contractFunctions
 */

// TESTCASE NUMBER: 1
fun case_1() {
    funWithExactlyOnceCallsInPlace {
        throw Exception()
    }
    println("1")
}

// TESTCASE NUMBER: 2
fun case_2() {
    funWithAtLeastOnceCallsInPlace {
        throw Exception()
    }
    println("1")
}

// TESTCASE NUMBER: 3
fun case_3() {
    funWithExactlyOnceCallsInPlace {
        return
    }
    println("1")
}

// TESTCASE NUMBER: 4
fun case_4() {
    funWithAtLeastOnceCallsInPlace {
        return
    }
    println("1")
}

// TESTCASE NUMBER: 5
fun case_5(args: Array<String>) {
    fun nestedFun_1() {
        funWithAtLeastOnceCallsInPlace {
            return@nestedFun_1
        }
        println("1")
    }
    fun nestedFun_2() {
        args.forEach {
            funWithAtLeastOnceCallsInPlace {
                return@forEach
            }
            println("1")
        }
    }
    fun nestedFun_3() {
        fun nestedFun_4() {
            funWithAtLeastOnceCallsInPlace {
                return@nestedFun_4
            }
            println("1")
        }
        println("1")
    }
}

// TESTCASE NUMBER: 6
fun case_6(args: Array<String>) {
    args.forEach {
        funWithExactlyOnceCallsInPlace {
            return@forEach
        }
        println("1")
    }
    args.forEach {
        fun nestedFun_1() {
            funWithExactlyOnceCallsInPlace {
                return@nestedFun_1
            }
            println("1")
        }
    }
    args.forEach {
        fun nestedFun_2() {
            funWithExactlyOnceCallsInPlace {
                return
            }
            println("1")
        }
    }
}

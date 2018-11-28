// !DIAGNOSTICS: -UNUSED_VARIABLE
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !WITH_CONTRACT_FUNCTIONS
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: contracts, analysis, controlFlow, unreachableCode
 * NUMBER: 1
 * DESCRIPTION: Unreachable code detection using contract function with CallsInPlace effect
 */

// TESTCASE NUMBER: 1
fun case_1() {
    funWithExactlyOnceCallsInPlace {
        throw Exception()
    }
    <!UNREACHABLE_CODE!>println("1")<!>
}

// TESTCASE NUMBER: 2
fun case_2() {
    funWithAtLeastOnceCallsInPlace {
        throw Exception()
    }
    <!UNREACHABLE_CODE!>println("1")<!>
}

// TESTCASE NUMBER: 3
fun case_3() {
    funWithExactlyOnceCallsInPlace {
        return
    }
    <!UNREACHABLE_CODE!>println("1")<!>
}

// TESTCASE NUMBER: 4
fun case_4() {
    funWithAtLeastOnceCallsInPlace {
        return
    }
    <!UNREACHABLE_CODE!>println("1")<!>
}

// TESTCASE NUMBER: 5
fun case_5(args: Array<String>) {
    fun nestedFun_1() {
        funWithAtLeastOnceCallsInPlace {
            return@nestedFun_1
        }
        <!UNREACHABLE_CODE!>println("1")<!>
    }
    fun nestedFun_2() {
        args.forEach {
            funWithAtLeastOnceCallsInPlace {
                return@forEach
            }
            <!UNREACHABLE_CODE!>println("1")<!>
        }
    }
    fun nestedFun_3() {
        fun nestedFun_4() {
            funWithAtLeastOnceCallsInPlace {
                return@nestedFun_4
            }
            <!UNREACHABLE_CODE!>println("1")<!>
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
        <!UNREACHABLE_CODE!>println("1")<!>
    }
    args.forEach {
        fun nestedFun_1() {
            funWithExactlyOnceCallsInPlace {
                return@nestedFun_1
            }
            <!UNREACHABLE_CODE!>println("1")<!>
        }
    }
    args.forEach {
        fun nestedFun_2() {
            funWithExactlyOnceCallsInPlace {
                return
            }
            <!UNREACHABLE_CODE!>println("1")<!>
        }
    }
}

// TESTCASE NUMBER: 7
fun case_7() {
    <!UNREACHABLE_CODE!>val value_1 =<!> funWithExactlyOnceCallsInPlace {
        throw Exception()
        <!UNREACHABLE_CODE!>println(1)<!>
    }
    <!UNREACHABLE_CODE!>println(value_1)<!>
}


// TESTCASE NUMBER: 8
fun case_8() {
    <!UNREACHABLE_CODE!>println(<!>funWithExactlyOnceCallsInPlace { return; <!UNREACHABLE_CODE!>println(1)<!> }<!UNREACHABLE_CODE!>)<!>
    <!UNREACHABLE_CODE!>return<!>
}

// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !WITH_CONTRACT_FUNCTIONS
// SKIP_TXT

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)

 SECTION: contracts
 CATEGORY: analysis, controlFlow, unreachableCode
 NUMBER: 2
 DESCRIPTION: Check for lack of unreachable code report when 'at most once' and 'unknown' invokations in CallsInPlace effect used.
 */

fun case_1() {
    funWithAtMostOnceCallsInPlace {
        throw Exception()
    }
    funWithAtMostOnceCallsInPlace {
        return
    }
    println("1")
}

fun case_2() {
    funWithUnknownCallsInPlace {
        throw Exception()
    }
    funWithUnknownCallsInPlace {
        return
    }
    println("1")
}

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

// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !WITH_CONTRACT_FUNCTIONS
// SKIP_TXT

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)

 SECTION: contracts
 CATEGORY: analysis, controlFlow, unreachableCode
 NUMBER: 4
 DESCRIPTION: Unreachable code detection using nested contract functions with CallsInPlace effect
 */

fun case_1() {
    funWithExactlyOnceCallsInPlace {
        funWithExactlyOnceCallsInPlace {
            funWithExactlyOnceCallsInPlace {
                throw Exception()
            }
            <!UNREACHABLE_CODE!>println("1")<!>
        }
        <!UNREACHABLE_CODE!>println("2")<!>
    }
    <!UNREACHABLE_CODE!>println("3")<!>
}

fun case_2() {
    funWithAtLeastOnceCallsInPlace {
        funWithAtLeastOnceCallsInPlace label_1@ {
            funWithAtLeastOnceCallsInPlace {
                return@label_1
            }
            <!UNREACHABLE_CODE!>println("1")<!>
        }
        println("2")
    }
    funWithAtLeastOnceCallsInPlace {
        return
    }
    <!UNREACHABLE_CODE!>println("3")<!>
}

fun case_3() {
    funWithExactlyOnceCallsInPlace {
        funWithExactlyOnceCallsInPlace {
            funWithExactlyOnceCallsInPlace {
                return<!LABEL_NAME_CLASH!>@funWithExactlyOnceCallsInPlace<!>
            }
            println("1")
        }
        println("2")
    }
    println("3")
}

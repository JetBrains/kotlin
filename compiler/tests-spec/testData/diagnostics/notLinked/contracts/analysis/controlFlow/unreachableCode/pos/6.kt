// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !WITH_CONTRACT_FUNCTIONS
// SKIP_TXT

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)

 SECTION: contracts
 CATEGORY: analysis, controlFlow, unreachableCode
 NUMBER: 6
 DESCRIPTION: Check for lack of unreachable code report when in complex control flow of contract function lambda not all branches are doing non-local return
 */

fun case_1(b: Boolean?, c: Boolean) {
    funWithExactlyOnceCallsInPlace {
        if (b == null) return

        try {
            <!DEBUG_INFO_IMPLICIT_EXHAUSTIVE!>when (<!DEBUG_INFO_SMARTCAST!>b<!>) {
                true -> {
                    println(1)
                    return
                }
                false -> {
                    println(2)
                    throw Exception()
                }
            }<!>
        } catch (e: Exception) {
            if (c) {
                return@funWithExactlyOnceCallsInPlace
            } else {
                return
            }
        }
        <!UNREACHABLE_CODE!>println(3)<!>
    }
    println(3)
}

fun case_2(b: Boolean?, c: Boolean) {
    funWithAtLeastOnceCallsInPlace {
        when (b) {
            true -> {
                println(1)
                return
            }
            else -> {
                if (b == null) {
                    funWithExactlyOnceCallsInPlace {
                        when {
                            c == true -> throw Exception()
                            else -> funWithAtMostOnceCallsInPlace { return }
                        }
                        println(3)
                    }
                    println(3)
                } else {
                    throw Exception()
                }
                println(3)
            }
        }
        println(3)
    }
    println(3)
}

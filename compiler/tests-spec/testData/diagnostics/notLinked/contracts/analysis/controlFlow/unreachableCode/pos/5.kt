// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !WITH_CONTRACT_FUNCTIONS
// SKIP_TXT

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)

 SECTION: contracts
 CATEGORY: analysis, controlFlow, unreachableCode
 NUMBER: 5
 DESCRIPTION: Unreachable code detection using contract functions with complex control flow inside
 */

fun case_1(b: Boolean?) {
    funWithExactlyOnceCallsInPlace {
        if (b == null) return

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
        <!UNREACHABLE_CODE!>println(3)<!>
    }
    <!UNREACHABLE_CODE!>println(3)<!>
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
                            else -> funWithAtLeastOnceCallsInPlace { return }
                        }
                        <!UNREACHABLE_CODE!>println(3)<!>
                    }
                    <!UNREACHABLE_CODE!>println(3)<!>
                } else {
                    throw Exception()
                }
                <!UNREACHABLE_CODE!>println(3)<!>
            }
        }
        <!UNREACHABLE_CODE!>println(3)<!>
    }
    <!UNREACHABLE_CODE!>println(3)<!>
}

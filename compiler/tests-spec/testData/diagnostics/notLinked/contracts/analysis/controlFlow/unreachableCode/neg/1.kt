// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !WITH_CONTRACT_FUNCTIONS
// SKIP_TXT

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)

 SECTION: contracts
 CATEGORY: analysis, controlFlow, unreachableCode
 NUMBER: 1
 DESCRIPTION: Using not allowed break and continue inside lambda of contract function
 */

fun case_1(value_1: Boolean) {
    while (value_1) {
        funWithExactlyOnceCallsInPlace {
            <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!>
        }
        println("1")
    }

    loop@ for (i in 0..10) {
        funWithExactlyOnceCallsInPlace {
            <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break@loop<!>
        }
        println("1")
    }
}

fun case_2(value_1: Boolean) {
    for (i in 0..10) {
        funWithExactlyOnceCallsInPlace {
            <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!>
        }
        println("1")
    }

    loop@ while (value_1) {
        funWithExactlyOnceCallsInPlace {
            <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue@loop<!>
        }
        println("1")
    }
}

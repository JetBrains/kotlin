// LANGUAGE: -BreakContinueInInlineLambdas
// !OPT_IN: kotlin.contracts.ExperimentalContracts
// SKIP_TXT

// TESTCASE NUMBER: 1
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

// TESTCASE NUMBER: 2
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

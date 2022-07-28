// LANGUAGE: -BreakContinueInInlineLambdas


fun test() {
    var i = 0
    outer@ while (true) {
        i += 1
        inner@ for (j in 1..10) {
            {
                if (i == 2) {
                    <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue@outer<!>
                }

                if (i == 4) {
                    <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break@outer<!>
                }

                if (j == 2) {
                    <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!>
                }

                if (j == 4) {
                    <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue@inner<!>
                }

                if (j == 6 && i == 1) {
                    <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break@inner<!>
                }

                if (j == 6 && i == 3) {
                    <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!>
                }
            }()
        }
    }
}
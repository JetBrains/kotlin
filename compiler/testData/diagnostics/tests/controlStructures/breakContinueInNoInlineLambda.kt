// FIR_IDENTICAL
// LANGUAGE: +BreakContinueInInlineLambdas


<!NOTHING_TO_INLINE!>inline<!> fun foo(noinline block: () -> Unit) = block()


fun test1() {
    L1@ while (true) {
        foo { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!> }

        foo { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break@L1<!> }

        foo { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!> }

        foo { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue@L1<!> }
    }
}
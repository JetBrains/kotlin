// FIR_IDENTICAL
// LANGUAGE: +BreakContinueInInlineLambdas


inline fun notInlining(noinline block1: () -> Unit, block2: () -> Unit) {
    block1()
    block2()
}

fun test() {
    label@ while(true) {
        notInlining({ <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!> }) {}
        notInlining({ <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!> }) {}
        notInlining(fun () { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!> }) {}
        notInlining(fun () { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!> }) {}
        notInlining({ <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break@label<!> }) {}
        notInlining({ <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue@label<!> }) {}
        notInlining(fun () { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break@label<!> }) {}
        notInlining(fun () { <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue@label<!> }) {}
    }
}

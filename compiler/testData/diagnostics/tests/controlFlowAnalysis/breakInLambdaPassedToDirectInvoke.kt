// FIR_IDENTICAL
// LANGUAGE: +BreakContinueInInlineLambdas

fun test() {
    while(true) {
        {block: () -> Unit -> block()}(
            {<!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!>}
        )
    }
}
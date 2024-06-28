// FIR_IDENTICAL
// LANGUAGE: +BreakContinueInInlineLambdas
// ISSUE: KT-1436

inline fun test(
    b: Boolean,
    block: () -> Unit = {
        if (b) <!BREAK_OR_CONTINUE_OUTSIDE_A_LOOP!>break<!> else <!BREAK_OR_CONTINUE_OUTSIDE_A_LOOP!>continue<!>
    }
) {
    for (i in 0..10) {
        block()
    }
}
// FIR_IDENTICAL
// LANGUAGE: +BreakContinueInInlineLambdas
// ISSUE: KT-1436

fun noInline(s: ()->Unit) {
    s()
}

inline fun inline(s: ()->Unit) {
    s()
}

<!NOTHING_TO_INLINE!>inline<!> fun inlineWithNoInline(noinline block: () -> Unit): Unit = block()

inline fun inlineWithCrossInline(crossinline block: () -> Unit): Unit = block()

fun test1() {
    for (i in 1..10) {
        inline {
            noInline {
                <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!>
                <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!>
            }
        }
    }
}

fun test2() {
    for (i in 1..10) {
        noInline {
            inline {
                <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!>
                <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!>
            }
        }
    }
}

fun test3() {
    for (i in 1..10) {
        inline {
            inlineWithNoInline {
                <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!>
                <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!>
            }
        }
    }
}

fun test4() {
    for (i in 1..10) {
        inlineWithNoInline {
            inline {
                <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!>
                <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!>
            }
        }
    }
}

fun test5() {
    for (i in 1..10) {
        inline {
            inlineWithCrossInline{
                <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!>
                <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!>
            }
        }
    }
}

fun test6() {
    for (i in 1..10) {
        inlineWithCrossInline {
            inline {
                <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!>
                <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!>
            }
        }
    }
}
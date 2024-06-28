// LANGUAGE: +BreakContinueInInlineLambdas
// DIAGNOSTICS: -UNREACHABLE_CODE, -USELESS_ELVIS, -USELESS_IS_CHECK
// ISSUE: KT-1436

fun noInline(s: ()->Unit) {
    s()
}

inline fun inline(s: ()->Unit) {
    s()
}

fun test1(i: Int) {
    while (i < 10) {
        inline {
            if (i == 1) <!UNSUPPORTED_FEATURE!>continue<!>
            if (i == 2) <!UNSUPPORTED_FEATURE!>break<!>
        }
    }
}

fun test2(i: Int) {
    while (i < 10) {
        noInline {
            if (i == 1) <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!>
            if (i == 2) <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!>
        }
    }
}

fun test3(i: Int) {
    do {
        inline {
            if (i == 1) <!UNSUPPORTED_FEATURE!>continue<!>
            if (i == 2) <!UNSUPPORTED_FEATURE!>break<!>
        }
    } while(i < 10)
}

fun test4(i: Int) {
    do {
        noInline {
            if (i == 1) <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!>
            if (i == 2) <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!>
        }
    } while(i < 10)
}

fun test5() {
    for (i in 0..10) {
        inline {
            when {
                i == 0 -> <!UNSUPPORTED_FEATURE!>continue<!>
                i == 1 -> <!UNSUPPORTED_FEATURE!>break<!>
            }
        }
    }
}

fun test6() {
    for (i in 0..10) {
        noInline {
            when {
                i == 0 -> <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!>
                i == 1 -> <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!>
            }
        }
    }
}

fun test7(x: Any, b: Boolean){
    for (i in 0..10) {
        inline {
            when (x) {
                if (b) <!UNSUPPORTED_FEATURE!>continue<!> else <!UNSUPPORTED_FEATURE!>break<!> -> 1
            }
        }
    }
}

fun test8(x: Any, b: Boolean){
    for (i in 0..10) {
        noInline {
            when (x) {
                if (b) <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!> else <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!> -> 1
            }
        }
    }
}

fun test9() {
    for (i in 0..10) {
        inline {
            try {
                <!UNSUPPORTED_FEATURE!>continue<!>
            } catch (e: Exception) {
                <!UNSUPPORTED_FEATURE!>break<!>
            }
        }
    }
}

fun test10() {
    for (i in 0..10) {
        noInline {
            try {
                <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!>
            } catch (e: Exception) {
                <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!>
            }
        }
    }
}

fun test11(x: Any) {
    for (i in 0..10) {
        inline {
            x is String || <!UNSUPPORTED_FEATURE!>continue<!>
            x is Int || <!UNSUPPORTED_FEATURE!>break<!>
            x is Double && <!UNSUPPORTED_FEATURE!>continue<!>
            x is Float && <!UNSUPPORTED_FEATURE!>break<!>
        }
    }
}

fun test12(x: Any) {
    for (i in 0..10) {
        noInline {
            x is String || <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!>
            x is Int || <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!>
            x is Double && <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!>
            x is Float && <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!>
        }
    }
}

fun test13(x: Any?) {
    for (i in 0..10) {
        inline{
            x?: <!UNSUPPORTED_FEATURE!>continue<!>
            x?: <!UNSUPPORTED_FEATURE!>break<!>
        }
    }
}

fun test14(x: Any?) {
    for (i in 0..10) {
        noInline {
            x?: <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!>
            x?: <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!>
        }
    }
}

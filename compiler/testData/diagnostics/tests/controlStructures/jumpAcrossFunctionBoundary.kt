fun call(f: () -> Unit) = f()

fun f1() {
    outer@ while (true) {
        call {
            <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break@outer<!>
        }
    }
}

fun f2() {
    do {
        fun inner() {
            <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!>
        }
    } while (true)
}

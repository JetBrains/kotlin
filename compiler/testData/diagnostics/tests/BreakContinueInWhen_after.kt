// LANGUAGE: +AllowBreakAndContinueInsideWhen

fun breakContinueInWhen(i: Int) {
    for (y in 0..10) {
        when(i) {
            0 -> continue
            1 -> break
            2 -> {
                for(z in 0..10) {
                    break
                }
                for(w in 0..10) {
                    continue
                }
            }
        }
    }
}


fun breakContinueInWhenWithWhile(i: Int, j: Int) {
    while (i > 0) {
        when (i) {
            0 -> continue
            1 -> break
            2 -> {
                while (j > 0) {
                    break
                }
            }
        }
    }
}

fun breakContinueInWhenWithDoWhile(i: Int, j: Int) {
    do {
        when (i) {
            0 -> continue
            1 -> break
            2 -> {
                do {
                    if (j == 5) break
                    if (j == 10) continue
                } while (j > 0)
            }
        }
    } while (i > 0)
}

fun labeledBreakContinue(i: Int) {
    outer@ for (y in 0..10) {
        when (i) {
            0 -> continue@outer
            1 -> break@outer
        }
    }
}

fun testBreakContinueInWhenInWhileCondition() {
    var i = 0
    while (
        when (i) {
            1 -> <!BREAK_OR_CONTINUE_OUTSIDE_A_LOOP!>break<!>
            2 -> <!BREAK_OR_CONTINUE_OUTSIDE_A_LOOP!>continue<!>
            else -> true
        }
    ) {
        ++i
    }
}

fun testBreakContinueInWhenInDoWhileCondition() {
    var i = 0
    do {
        ++i
    } while (
        when (i) {
            1 -> <!BREAK_OR_CONTINUE_OUTSIDE_A_LOOP!>break<!>
            2 -> <!BREAK_OR_CONTINUE_OUTSIDE_A_LOOP!>continue<!>
            else -> true
        }
    )
}

fun testBreakContinueInWhenInForIteratorExpression(xs: List<Any>, i: Int) {
    for (x in when (i) {
        1 -> <!BREAK_OR_CONTINUE_OUTSIDE_A_LOOP!>break<!>
        2 -> <!BREAK_OR_CONTINUE_OUTSIDE_A_LOOP!>continue<!>
        else -> xs
    }) {
    }
}

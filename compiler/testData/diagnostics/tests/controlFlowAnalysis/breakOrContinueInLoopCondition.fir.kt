fun test() {

    l@ for (i in if (true) 1..10 else <!BREAK_OR_CONTINUE_OUTSIDE_A_LOOP!>continue@l<!>) {}
    for (i in if (true) 1..10 else <!BREAK_OR_CONTINUE_OUTSIDE_A_LOOP!>continue<!>) {}

    while (<!BREAK_OR_CONTINUE_OUTSIDE_A_LOOP!>break<!>) {}
    l@ while (<!BREAK_OR_CONTINUE_OUTSIDE_A_LOOP!>break@l<!>) {}

    do {} while (continue)
    l@ do {} while (continue@l)

    //KT-5704
    var i = 0
    while (if(i++ == 10) <!BREAK_OR_CONTINUE_OUTSIDE_A_LOOP!>break<!> else <!BREAK_OR_CONTINUE_OUTSIDE_A_LOOP!>continue<!>) {}
}

fun test2(b: Boolean) {
    while (b) {
        while (break) {}
    }

    do {
        while (continue) {}
    } while (b)

    while (b) {
        do {} while (break)
    }

    for (i in 1..10) {
        for (j in if (true) 1..10 else continue) {
        }
    }
}

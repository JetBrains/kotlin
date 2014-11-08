fun IntRange.map<T>(<!UNUSED_PARAMETER!>f<!>: (Int) -> T): List<T> = throw AssertionError("")

fun foo() {
    for (i in 1..3) yield {
        if (i == 2) <!BREAK_OR_CONTINUE_OUTSIDE_A_LOOP!>break<!>
        if (i == 1) <!BREAK_OR_CONTINUE_OUTSIDE_A_LOOP!>continue<!>
        i
    }
}
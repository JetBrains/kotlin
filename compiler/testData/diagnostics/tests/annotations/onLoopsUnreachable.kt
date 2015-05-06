fun test() {
    @ann
    while (2 > 1) {}

    @ann
    <!UNREACHABLE_CODE!>do {} while (2 > 1)<!>

    @ann
    <!UNREACHABLE_CODE!>for (i in 1..2) {}<!>
}

annotation class ann
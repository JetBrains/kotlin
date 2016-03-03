fun test1() {
    run {
        if (true) {
            <!INVALID_IF_AS_EXPRESSION!>if (true) {}<!>
        }
        else {
            1
        }
    }
}

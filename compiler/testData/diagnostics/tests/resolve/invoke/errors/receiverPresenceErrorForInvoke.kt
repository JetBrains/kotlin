fun test1(f: String.() -> Unit) {
    <!MISSING_RECEIVER!>(f)<!>()

    <!MISSING_RECEIVER!>f<!>()
}

fun test2(f: (Int) -> Int) {
    1.<!UNRESOLVED_REFERENCE!>f<!>(2)

    2.<!NO_RECEIVER_ALLOWED!>(f)<!>(2)
}
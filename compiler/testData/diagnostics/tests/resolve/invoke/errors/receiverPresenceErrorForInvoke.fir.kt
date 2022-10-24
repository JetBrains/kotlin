fun test1(f: String.() -> Unit) {
    <!NO_VALUE_FOR_PARAMETER!>(f)()<!>

    <!NO_VALUE_FOR_PARAMETER!>f()<!>
}

fun test2(f: (Int) -> Int) {
    1.<!UNRESOLVED_REFERENCE!>f<!>(2)

    2.(<!UNRESOLVED_REFERENCE!>f<!>)(2)
}

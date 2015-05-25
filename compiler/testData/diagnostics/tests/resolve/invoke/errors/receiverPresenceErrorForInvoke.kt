fun test1(f: String.() -> Unit) {
    <!MISSING_RECEIVER!>(f)<!>()

    <!MISSING_RECEIVER!>f<!>()
}

fun test2(f: (Int) -> Int) {
    1.<!FREE_FUNCTION_CALLED_AS_EXTENSION!>f<!>(<!TOO_MANY_ARGUMENTS!>2<!>)

    2.<!FREE_FUNCTION_CALLED_AS_EXTENSION!>(f)<!>(<!TOO_MANY_ARGUMENTS!>2<!>)
}

fun test1(f: String.() -> Unit) {
    <!MISSING_RECEIVER!>(f)<!>()

    <!MISSING_RECEIVER!>f<!>()
}

fun test2(f: (Int) -> Int) {
    1.<!INVOKE_EXTENSION_ON_NOT_EXTENSION_FUNCTION!>f<!>(<!TOO_MANY_ARGUMENTS!>2<!>)

    2.<!INVOKE_EXTENSION_ON_NOT_EXTENSION_FUNCTION!>(f)<!>(<!TOO_MANY_ARGUMENTS!>2<!>)
}
package d

class T {
    fun baz() = 1
}

<!ILLEGAL_MODIFIER!>override<!> fun zzz() {}

fun foo(t: T) {
    <!ILLEGAL_MODIFIER!>override<!> fun T.baz() = 2

    // was "Visibility is unknown yet exception"
    t.baz()

    zzz()
}

package d

class T {
    fun baz() = 1
}

<!WRONG_MODIFIER_TARGET!>override<!> fun zzz() {}

fun foo(t: T) {
    <!WRONG_MODIFIER_TARGET!>override<!> fun T.baz() = 2

    // was "Visibility is unknown yet exception"
    t.baz()

    zzz()
}

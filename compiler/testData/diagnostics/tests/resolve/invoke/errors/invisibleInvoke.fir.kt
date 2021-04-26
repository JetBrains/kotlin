// !DIAGNOSTICS: -UNUSED_PARAMETER

class My {
    private operator fun Int.invoke(s: String) {}
}

fun My.foo(i: Int) {
    <!INVISIBLE_REFERENCE!>i<!>("")
    <!INVISIBLE_REFERENCE!>1<!>("")
}

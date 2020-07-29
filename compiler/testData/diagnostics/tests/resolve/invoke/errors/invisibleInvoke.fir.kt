// !DIAGNOSTICS: -UNUSED_PARAMETER

class My {
    private operator fun Int.invoke(s: String) {}
}

fun My.foo(i: Int) {
    <!HIDDEN!>i<!>("")
    <!HIDDEN!>1("")<!>
}

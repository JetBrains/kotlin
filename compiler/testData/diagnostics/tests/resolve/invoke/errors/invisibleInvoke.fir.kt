// !DIAGNOSTICS: -UNUSED_PARAMETER

class My {
    private operator fun Int.invoke(s: String) {}
}

fun My.foo(i: Int) {
    <!INAPPLICABLE_CANDIDATE!>i<!>("")
    <!INAPPLICABLE_CANDIDATE!>1("")<!>
}

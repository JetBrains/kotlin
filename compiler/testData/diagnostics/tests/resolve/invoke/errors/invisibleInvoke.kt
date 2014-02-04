// !DIAGNOSTICS: -UNUSED_PARAMETER

class My {
    private fun Int.invoke(s: String) {}
}

fun My.foo(i: Int) {
    <!INVISIBLE_MEMBER!>i<!>("")
    <!INVISIBLE_MEMBER!>1<!>("")
}

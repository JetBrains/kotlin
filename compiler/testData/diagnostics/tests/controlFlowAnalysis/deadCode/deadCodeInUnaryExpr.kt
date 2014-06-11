fun testPrefix() {
    fun Any.not() {}
    <!UNREACHABLE_CODE!>!<!>todo()
}

fun testPostfixWithCall(n: Nothing) {
    fun Nothing.inc(): Nothing = this
    n<!UNREACHABLE_CODE!>++<!>
}

fun testPostfixSpecial() {
    todo()<!UNNECESSARY_NOT_NULL_ASSERTION, UNREACHABLE_CODE!>!!<!>
}

fun todo() = throw Exception()
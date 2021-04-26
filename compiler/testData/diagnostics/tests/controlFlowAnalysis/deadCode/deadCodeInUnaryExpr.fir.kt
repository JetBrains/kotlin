fun testPrefix() {
    operator fun Any.not() {}
    !todo()
}

fun testPostfixWithCall(n: Nothing) {
    operator fun Nothing.inc(): Nothing = this
    n++
}

fun testPostfixSpecial() {
    todo()<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
}

fun todo(): Nothing = throw Exception()

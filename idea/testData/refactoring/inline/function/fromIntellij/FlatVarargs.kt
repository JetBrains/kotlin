fun xxx() {
    foo(*arrayOf("aa", "hh"))
}

fun <caret>foo(vararg ss: String) {
    bar(*ss)
}

fun bar(s: String, ss: String) {

}

fun bar(vararg ss: String) {}

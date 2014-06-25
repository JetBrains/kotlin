fun testFor() {
    fun Nothing.iterator() = (0..1).iterator()

    <!UNREACHABLE_CODE!>for (i in<!> todo()<!UNREACHABLE_CODE!>) {}<!>
}

fun testWhile() {
    <!UNREACHABLE_CODE!>while (<!>todo()<!UNREACHABLE_CODE!>) {
    }<!>
}

fun testDoWhile() {
    do {

    } while(todo())

    <!UNREACHABLE_CODE!>bar()<!>
}

fun todo() = throw Exception()
fun bar() {}
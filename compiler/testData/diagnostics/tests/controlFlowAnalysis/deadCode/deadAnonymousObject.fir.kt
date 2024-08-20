// ISSUE: KT-70808

fun nonTerminating(): Nothing = throw RuntimeException()

class C {
    val x: String = nonTerminating()

    val o = <!UNREACHABLE_CODE!>object {
        val a = 0
        val b = 0
    }<!>
}

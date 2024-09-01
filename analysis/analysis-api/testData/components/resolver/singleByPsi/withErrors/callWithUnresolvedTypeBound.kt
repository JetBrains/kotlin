fun <T : NonExistent> foo(x: Int): T = null!!

fun test(x: Int) {
    val x: String = <expr>foo(x)</expr>
}

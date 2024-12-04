fun <T> foo(x: Int): Unresolved = null!!

fun test(x: Int) {
    val x: String = <expr>foo(x)</expr>
}

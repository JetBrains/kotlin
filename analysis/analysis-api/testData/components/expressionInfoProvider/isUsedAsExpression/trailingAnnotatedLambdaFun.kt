fun foo(lambda: (String) -> Int): Int = lambda("jello!")

fun test() {
    <expr>foo</expr> @Deprecated("") {
        it.length
    }
}
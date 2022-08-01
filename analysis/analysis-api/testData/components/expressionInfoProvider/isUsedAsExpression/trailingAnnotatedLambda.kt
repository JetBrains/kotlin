fun foo(lambda: (String) -> Int): Int = lambda("jello!")

fun test() {
    <expr>foo @Deprecated("") {
        it.length
    }</expr>
}
fun foo(lambda: (String) -> Int): Int = lambda("jello!")

fun test() {
    foo @Deprecated("") {<expr>
        it.length
    </expr>}
}
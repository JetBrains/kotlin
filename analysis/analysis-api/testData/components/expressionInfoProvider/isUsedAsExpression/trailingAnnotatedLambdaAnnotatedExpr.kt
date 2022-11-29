fun foo(lambda: (String) -> Int): Int = lambda("jello!")

fun test() {
    foo <expr>@Deprecated("") {
        it.length
    }</expr>
}
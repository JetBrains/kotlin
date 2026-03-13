// LANGUAGE: +NameBasedDestructuring
data class Tuple(val first: String, val second: Int)

fun lambda() {
    fun foo(f: (Tuple) -> Unit) {}

    foo { [<expr>val first: String</expr>] -> }
}

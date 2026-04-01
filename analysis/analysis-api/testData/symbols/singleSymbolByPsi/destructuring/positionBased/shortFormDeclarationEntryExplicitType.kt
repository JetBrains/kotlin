// LANGUAGE: +NameBasedDestructuring
data class Tuple(val first: String, val second: Int)

fun declaration(x: Tuple) {
    if (true) {
        val [<expr>first: String</expr>] = x
    }
}

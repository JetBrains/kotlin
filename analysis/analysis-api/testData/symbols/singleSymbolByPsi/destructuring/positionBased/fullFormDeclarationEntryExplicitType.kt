// LANGUAGE: +NameBasedDestructuring
data class Tuple(val first: String, val second: Int)

fun declaration(x: Tuple) {
    if (true) { [<expr>val first: String</expr>] = x }
}

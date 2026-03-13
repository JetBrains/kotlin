// LANGUAGE: +NameBasedDestructuring
data class Tuple(val first: String, val second: Int)

fun loop(x: List<Tuple>) {
    for ([<expr>val first</expr>] in x) {}
}

// LANGUAGE: +NameBasedDestructuring

class Tuple(val first: String, val second: Int)

fun declaration(x: Tuple) {
    if (true) {
        (<expr>val _ = first</expr>, var second,) = x
    }
}

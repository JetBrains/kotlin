// LANGUAGE: +NameBasedDestructuring

class Tuple(val first: String, val second: Int)

fun declaration(x: Tuple) {
    if (true) {
        (val first, <expr>var second</expr>,) = x
    }
}

// LANGUAGE: +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm
class Tuple(val first: String, val second: Int)

fun loop(x: List<Tuple>) {
    for ((<expr>first: String</expr>, second,) in x) {}
}

// LANGUAGE: +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm
class Tuple(val first: String, val second: Int)

fun loop(x: List<Tuple>) {
    for ((<expr>_ = first</expr>, second,) in x) {}
}

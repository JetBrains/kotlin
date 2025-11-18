// LANGUAGE: +NameBasedDestructuring
data class Tuple(val first: String, val second: Int)

fun declaration(x: Tuple) {
    if (true) { [val first, var second,] = x }
    if (true) { [var first] = x }
    if (true) { [val first: String] = x }
}

fun loop(x: List<Tuple>) {
    for ([val first, val second,] in x) {}
    for ([val first] in x) {}
    for ([val first: String] in x) {}
}

fun lambda() {
    fun foo(f: (Tuple) -> Unit) {}

    foo { [val first, val second,] -> }
    foo { [val first] -> }
    foo { [val first: String] -> }
}

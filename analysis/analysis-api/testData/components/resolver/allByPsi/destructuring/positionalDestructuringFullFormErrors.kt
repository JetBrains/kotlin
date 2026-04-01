// LANGUAGE: +NameBasedDestructuring
// COMPILATION_ERRORS
data class Tuple(val first: String, val second: Int)
class NonDataTuple(val first: String, val second: Int)

fun declaration(x: Tuple, y: NonDataTuple) {
    if (true) { [val first, var second: String,] = x }
    if (true) { [var first: Int] = x }
    if (true) { [val first] = y }
    if (true) { [val first] }
}

fun loop(x: List<Tuple>, y: NonDataTuple) {
    for ([val first, val second: String,] in x) {}
    for ([val first: Int] in x) {}
    for ([val first: String] in y) {}
}

fun lambda() {
    fun foo(f: (Tuple) -> Unit) {}
    fun bar(f: (NonDataTuple) -> Unit) {}

    foo { [val first, val second: String,] -> }
    foo { [val first: Int] -> }
    bar { [val first] -> }
}

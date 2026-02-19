// LANGUAGE: +NameBasedDestructuring
// COMPILATION_ERRORS
data class Tuple(val first: String, val second: Int)
class NonDataTuple(val first: String, val second: Int)

fun declaration(x: Tuple, y: NonDataTuple) {
    if (true) { val [first, second: String,] = x }
    if (true) { var [first: Int] = x }
    if (true) { val [first] = y }
    if (true) { val [first] }
}

fun loop(x: List<Tuple>, y: NonDataTuple) {
    for ([first, second: String,] in x) {}
    for ([first: Int] in x) {}
    for ([first: String] in y) {}
}

fun lambda() {
    fun foo(f: (Tuple) -> Unit) {}
    fun bar(f: (NonDataTuple) -> Unit) {}

    foo { [first, second: String,] -> }
    foo { [first: Int] -> }
    bar { [first] -> }
}

// LANGUAGE: +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm
// COMPILATION_ERRORS
class Tuple(val a: String, val b: Int)

fun declaration(x: Tuple) {
    if (true) { val (a, second,) = x }
    if (true) { var (first) = x }
    if (true) { val (first: String) = x }
    if (true) { val (first) }
    if (true) { val (a: Int, b: String) = x }
    if (true) { val (first: Int = a, second: String = b) = x }
    if (true) { val (_) = x }
    if (true) { val (_: String) = x }
    // renaming
    if (true) { val (aa = first) = x }
    if (true) { val (aa: String = first) = x }
    if (true) { val (a = first) }
    if (true) { val (first: Int = a, second: String = b) = x }
}

fun loop(x: List<Tuple>) {
    for ((first, second,) in x) {}
    for ((first) in x) {}
    for ((first: String) in x) {}
    for ((a: Int, b: String) in x) {}
    for ((_) in x) {}
    for ((_: String) in x) {}

    // renaming
    for ((aa = first) in x) {}
    for ((aa: String = first) in x) {}
    for ((first: Int = a, second: String = b) in x) {}
}

fun lambda() {
    fun foo(f: (Tuple) -> Unit) {}

    foo { (first, second,) -> }
    foo { (first) -> }
    foo { (first: String) -> }
    foo { (a: Int, b: String) -> }
    foo { (_) -> }
    foo { (_: String) -> }

    // renaming
    foo { (aa = first) -> }
    foo { (aa: String = first) -> }
    foo { (first: Int = a, second: String = b) -> }
}

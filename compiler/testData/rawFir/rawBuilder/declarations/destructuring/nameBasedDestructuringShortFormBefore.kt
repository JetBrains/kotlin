// LANGUAGE: -EnableNameBasedDestructuringShortForm
class Tuple(val first: String, val second: Int)

fun declaration(x: Tuple) {
    if (true) { val (first, second,) = x }
    if (true) { var (first) = x }
    if (true) { val (first: String) = x }
    // leads to error when LF is disabled
    if (true) { val (aa = first) = x }
    if (true) { val (aa: String = first) = x }
}

fun loop(x: List<Tuple>) {
    for ((first, second,) in x) {}
    for ((first) in x) {}
    for ((first: String) in x) {}

    // leads to error when LF is disabled
    for ((aa = first) in x) {}
    for ((aa: String = first) in x) {}
}

fun lambda() {
    fun foo(f: (Tuple) -> Unit)

    foo { (first, second,) -> }
    foo { (first) -> }
    foo { (first: String) -> }

    // leads to error when LF is disabled
    foo { (aa = first) -> }
    foo { (aa: String = first) -> }
}
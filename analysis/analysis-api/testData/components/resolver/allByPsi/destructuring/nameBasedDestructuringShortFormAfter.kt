// LANGUAGE: +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm
class Tuple(val first: String, val second: Int)

fun declaration(x: Tuple) {
    if (true) { val (first, second,) = x }
    if (true) { val (second, first,) = x }
    if (true) { var (first) = x }
    if (true) { val (first: String) = x }
    // renaming
    if (true) { val (aa = first) = x }
    if (true) { val (aa: String = first) = x }
    if (true) { val (_ = first) = x }
    if (true) { val (_: String = first) = x }
}

fun loop(x: List<Tuple>) {
    for ((first, second,) in x) {}
    for ((second, first,) in x) {}
    for ((first) in x) {}
    for ((first: String) in x) {}

    // renaming
    for ((aa = first) in x) {}
    for ((aa: String = first) in x) {}
    for ((_ = first) in x) {}
    for ((_: String = first) in x) {}
}

fun lambda() {
    fun foo(f: (Tuple) -> Unit) {}

    foo { (first, second,) -> }
    foo { (second, first,) -> }
    foo { (first) -> }
    foo { (first: String) -> }

    // renaming
    foo { (aa = first) -> }
    foo { (aa: String = first) -> }
    foo { (_ = first) -> }
    foo { (_: String = first) -> }
}

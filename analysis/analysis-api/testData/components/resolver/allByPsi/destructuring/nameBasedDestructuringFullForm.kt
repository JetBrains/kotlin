// LANGUAGE: +NameBasedDestructuring

class Tuple(val first: String, val second: Int)

fun declaration(x: Tuple) {
    if (true) { (val first, var second,) = x }
    if (true) { (var first) = x }
    if (true) { (val first: String) = x }
    if (true) { (val aa = first) = x }
    if (true) { (val aa: String = first) = x }
    if (true) { (val _ = first) = x }
    if (true) { (val _: String = first) = x }
}

fun loop(x: List<Tuple>) {
    for ((val first, val second,) in x) {}
    for ((val first) in x) {}
    for ((val first: String) in x) {}
    for ((val aa = first) in x) {}
    for ((val aa: String = first) in x) {}
    for ((val _ = first) in x) {}
    for ((val _: String = first) in x) {}
}

fun lambda() {
    fun foo(f: (Tuple) -> Unit) {}

    foo { (val first, val second,) -> }
    foo { (val first) -> }
    foo { (val first: String) -> }
    foo { (val aa = first) -> }
    foo { (val aa: String = first) -> }
    foo { (val _ = first) -> }
    foo { (val _: String = first) -> }
}

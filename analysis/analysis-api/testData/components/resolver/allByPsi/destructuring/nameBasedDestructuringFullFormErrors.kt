// LANGUAGE: +NameBasedDestructuring
// COMPILATION_ERRORS

class Tuple(val a: String, val b: Int)

fun declaration(x: Tuple) {
    if (true) { (val a, var second,) = x }
    if (true) { (var first) = x }
    if (true) { (val first: String) = x }
    if (true) { (val aa = first) = x }
    if (true) { (val aa: String = first) = x }
    if (true) { (val first) }
    if (true) { (val aa: String = first) }
    if (true) { (val a: Int, val b: String) = x }
    if (true) { (val first: Int = a, val second: String = b) = x }
    if (true) { (val _) = x }
    if (true) { (val _: String) = x }
}

fun loop(x: List<Tuple>) {
    for ((val first, val second,) in x) {}
    for ((val first) in x) {}
    for ((val first: String) in x) {}
    for ((val aa = first) in x) {}
    for ((val aa: String = first) in x) {}
    for ((val a: Int, val b: String) in x) {}
    for ((val first: Int = a, val second: String = b) in x) {}
    for ((val _) in x) {}
    for ((val _: String) in x) {}
}

fun lambda() {
    fun foo(f: (Tuple) -> Unit) {}

    foo { (val first, val second,) -> }
    foo { (val first) -> }
    foo { (val first: String) -> }
    foo { (val aa = first) -> }
    foo { (val aa: String = first) -> }
    foo { (val a: Int, val b: String) -> }
    foo { (val first: Int = a, val second: String = b) -> }
    foo { (val _) -> }
    foo { (val _: String) -> }
}

// LANGUAGE: +NameBasedDestructuring

data class Box(val first: String, val second: String)

fun declaration(x: Box) {
    if (true) { [val first, var second,] = x }
    if (true) { [var first] = x }
    if (true) { [val first: String] = x }
}

fun loop(x: List<Box>) {
    for ([val first, val second,] in x) {}
    for ([val first] in x) {}
    for ([val first: String] in x) {}
}

fun lambda() {
    fun foo(f: (Box) -> Unit) {}

    foo { [val first, val second,] -> }
    foo { [val first] -> }
    foo { [val first: String] -> }
}
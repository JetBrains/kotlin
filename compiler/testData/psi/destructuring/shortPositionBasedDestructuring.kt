// LANGUAGE: +NameBasedDestructuring

data class Box(val first: String, val second: String)

fun declaration(x: Box) {
    if (true) { val [first, second,] = x }
    if (true) { var [first] = x }
    if (true) { val [first: String] = x }
}

fun loop(x: List<Box>) {
    for ([first, second,] in x) {}
    for ([first] in x) {}
    for ([first: String] in x) {}
}

fun lambda() {
    fun foo(f: (Box) -> Unit) {}

    foo { [first, second,] -> }
    foo { [first] -> }
    foo { [first: String] -> }
}
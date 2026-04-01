// LANGUAGE: +NameBasedDestructuring +DeprecateNameMismatchInShortDestructuringWithParentheses +EnableNameBasedDestructuringShortForm

data class Box(val first: String, val second: String)

fun declaration(x: Box) {
    if (true) { (val first, var second,) = x }
    if (true) { (var first) = x }
    if (true) { (val first: String) = x }
    if (true) { (val aa = first) = x }
    if (true) { (val aa: String = first) = x }
}

fun loop(x: List<Box>) {
    for ((val first, val second,) in x) {}
    for ((val first) in x) {}
    for ((val first: String) in x) {}
    for ((val aa = first) in x) {}
    for ((val aa: String = first) in x) {}
}

fun lambda() {
    fun foo(f: (Box) -> Unit) {}

    foo { (val first, val second,) -> }
    foo { (val first) -> }
    foo { (val first: String) -> }
    foo { (val aa = first) -> }
    foo { (val aa: String = first) -> }
}
// LANGUAGE: +NameBasedDestructuring +DeprecateNameMismatchInShortDestructuringWithParentheses +EnableNameBasedDestructuringShortForm

data class Box(val first: String)

fun declaration(x: Box) {
    if (true) { val (aa = first) = x }
    if (true) { val (aa: String = first) = x }
}

fun loop(x: List<Box>) {
    for ((aa = first) in x) {}
    for ((aa: String = first) in x) {}
}

fun lambda() {
    fun foo(f: (Box) -> Unit) {}

    foo { (aa = first) -> }
    foo { (aa: String = first) -> }
}
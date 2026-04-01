// LANGUAGE: +NameBasedDestructuring +DeprecateNameMismatchInShortDestructuringWithParentheses +EnableNameBasedDestructuringShortForm

data class Box(val first: String)

annotation class Ann(val x: String = "")

fun test1(x: Box) {
    if (true) { @Ann (var first) = x }
    if (true) { @Ann(var first) = x }
    if (true) { @Ann() (var first) = x }
    if (true) { @Ann("") (var first) = x }
    if (true) { @Ann(val first) = x }
    if (true) { @Ann() (val first) = x }
    if (true) { @Ann("") (val first) = x }
}
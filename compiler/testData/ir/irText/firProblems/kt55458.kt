// LANGUAGE: +NameBasedDestructuring +DeprecateNameMismatchInShortDestructuringWithParentheses +EnableNameBasedDestructuringShortForm
// WITH_STDLIB

fun runMe() {
    val [a: Any, _] = 1 to 2
    println(a)
}

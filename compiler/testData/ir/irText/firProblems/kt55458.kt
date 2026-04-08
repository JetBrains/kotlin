// LANGUAGE: +NameBasedDestructuring +DeprecateNameMismatchInShortDestructuringWithParentheses +EnableNameBasedDestructuringShortForm
// WITH_STDLIB

// KT-61141: `println (message: kotlin.Any?)` instead of `println (message: kotlin.Int)`

fun runMe() {
    val [a: Any, _] = 1 to 2
    println(a)
}

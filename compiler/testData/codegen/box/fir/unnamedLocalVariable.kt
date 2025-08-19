// LANGUAGE: +UnnamedLocalVariables +NameBasedDestructuring +DeprecateNameMismatchInShortDestructuringWithParentheses +EnableNameBasedDestructuringShortForm
// IGNORE_BACKEND_K1: ANY

var result = "FAIL: call() must be called"

fun call() {
    result = "OK"
}

object Structure {
    operator fun component1() {
        result = "FAIL: component1() must not be called"
    }
}

fun box(): String {
    val _ = call()
    val [_] = Structure
    return result
}

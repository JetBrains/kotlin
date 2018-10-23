// PROBLEM: none
// ERROR: Type mismatch: inferred type is List<Any> but List<String> was expected
// WITH_RUNTIME
fun test() {
    var list = listOf("")
    list <caret>+= 1
}
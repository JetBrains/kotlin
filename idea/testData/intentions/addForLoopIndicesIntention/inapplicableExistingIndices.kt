//IS_APPLICABLE: FALSE
// WITH_RUNTIME
fun b(c: List<String>) {
    for ((<caret>indexVariable, d) in c.withIndices()) {

    }
}
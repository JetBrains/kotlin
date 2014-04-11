//IS_APPLICABLE: FALSE
//ERROR: Unresolved reference: withIndices
fun b(c: List<String>) {
    for ((<caret>indexVariable, d) in c.withIndices()) {

    }
}
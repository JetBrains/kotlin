// IS_APPLICABLE: FALSE
//ERROR: Unresolved reference: withIndices
fun foo(b: List<Int>) {
    for ((i, <caret>c) in b.withIndices()) {
        return i
    }
}
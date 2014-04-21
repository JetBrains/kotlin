//ERROR: Unresolved reference: withIndices
fun foo(bar: List<String>) {
    for ((i,<caret>a) in bar.withIndices()) {

    }
}
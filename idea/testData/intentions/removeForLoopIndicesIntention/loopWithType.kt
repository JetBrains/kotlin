//ERROR: Unresolved reference: withIndices
fun foo(bar: List<Int>) {
    for ((i : <caret>Int, b: Int) in bar.withIndices()) {

    }
}
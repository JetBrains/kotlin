// IS_APPLICABLE: FALSE
//WITH_RUNTIME

fun foo(b: List<Int>) : Int {
    for ((i, <caret>c) in b.withIndices()) {
        return i
    }
    return 0
}
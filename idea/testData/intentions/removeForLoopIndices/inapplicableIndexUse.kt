// IS_APPLICABLE: FALSE
// WITH_RUNTIME

fun foo(b: List<Int>) : Int {
    for ((<caret>i, c) in b.withIndex()) {
        return i
    }
    return 0
}
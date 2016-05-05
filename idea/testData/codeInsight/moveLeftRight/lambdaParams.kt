// MOVE: left
fun foo(list: List<String>) {
    list.foldRightIndexed(1, { p1, <caret>p2, p3 -> 1 })
}
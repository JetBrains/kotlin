// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'mapIndexed{}.sum()'"
// INTENTION_TEXT_2: "Replace with 'asSequence().mapIndexed{}.sum()'"
fun foo(list: List<Int>): Int {
    var s = 0
    <caret>for ((index, item) in list.withIndex()) {
        s += item * index
    }
    return s
}

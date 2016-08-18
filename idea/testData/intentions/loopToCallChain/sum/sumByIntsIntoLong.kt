// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'map{}.sum()'"
// INTENTION_TEXT_2: "Replace with 'asSequence().map{}.sum()'"
fun foo(list: List<Int>): Long {
    var s = 0L
    <caret>for (item in list) {
        s += item
    }
    return s
}

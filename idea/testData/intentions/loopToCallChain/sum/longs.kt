// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'sum()'"
// IS_APPLICABLE_2: false
fun foo(list: List<Long>): Long {
    var s = 0L
    <caret>for (item in list) {
        s += item
    }
    return s
}

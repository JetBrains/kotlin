// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'sumByLong{}'"
// IS_APPLICABLE_2: false
fun foo(list: List<String>): Long {
    var s = 0L
    <caret>for (item in list) {
        s += item.length
    }
    return s
}

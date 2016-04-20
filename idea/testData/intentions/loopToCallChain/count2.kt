// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'count()'"
fun foo(list: Iterable<String>): Int {
    var count = 0
    <caret>for (s in list) {
        count++
    }
    return count
}
// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'count{}'"
fun foo(list: List<String>): Int {
    var count = 0
    <caret>for (s in list) {
        if (s.isNotBlank()) {
            ++count
        }
    }
    return count
}
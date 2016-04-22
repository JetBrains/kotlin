// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'indexOfFirst{}'"
fun foo(list: List<String>): Int {
    <caret>for ((index, s) in list.withIndex()) {
        if (s.length > 0) {
            return index
        }
    }
    return -1
}
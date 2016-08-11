// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'indexOf()'"
// IS_APPLICABLE_2: false
fun foo(list: List<String>): Int {
    <caret>for ((index, s) in list.withIndex()) {
        if (s == "a") {
            return index
        }
    }
    return -1
}
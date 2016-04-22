// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'contains()'"
fun foo(list: List<String>): Int {
    <caret>for (s in list) {
        if (s == "a") {
            return 1
        }
    }
    return 0
}
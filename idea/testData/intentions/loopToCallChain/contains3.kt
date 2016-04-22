// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'contains()'"
fun foo(list: List<String>) {
    var v = false
    <caret>for (s in list) {
        if (s == "a") {
            v = true
        }
    }
}
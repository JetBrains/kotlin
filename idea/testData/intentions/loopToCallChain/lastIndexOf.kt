// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'lastIndexOf()'"
fun foo(list: List<String>) {
    var result = -1
    <caret>for ((index, s) in list.withIndex()) {
        if (s == "a") {
            result = index
        }
    }
}
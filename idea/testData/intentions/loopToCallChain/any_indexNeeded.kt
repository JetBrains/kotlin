// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'filterIndexed{}.any()'"
fun foo(list: List<String>) {
    var found = false
    <caret>for ((index, s) in list.withIndex()) {
        if (s.length > index) {
            found = true
            break
        }
    }
}
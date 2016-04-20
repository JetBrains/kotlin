// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'any{}'"
fun foo(list: List<String>) {
    var found = false
    <caret>for (s in list) {
        if (s.length > 0) {
            found = true
        }
    }
}
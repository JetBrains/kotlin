// WITH_RUNTIME
// INTENTION_TEXT: "Replace with '+='"
fun foo(list: List<String>, target: MutableList<String>) {
    <caret>for (s in list) {
        target.add(s)
    }
}
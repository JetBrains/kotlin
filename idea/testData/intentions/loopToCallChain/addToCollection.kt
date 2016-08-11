// WITH_RUNTIME
// INTENTION_TEXT: "Replace with '+='"
// IS_APPLICABLE_2: false
fun foo(list: List<String>, target: MutableList<String>) {
    <caret>for (s in list) {
        target.add(s)
    }
}

// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'filterNotNullTo()'"
fun foo(list: List<String?>, target: MutableCollection<String>) {
    <caret>for (s in list) {
        if (s != null) {
            target.add(s)
        }
    }
}
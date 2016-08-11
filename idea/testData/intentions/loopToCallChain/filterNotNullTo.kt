// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'filterNotNullTo()'"
// IS_APPLICABLE_2: false
fun foo(list: List<String?>, target: MutableCollection<String>) {
    <caret>for (s in list) {
        if (s != null) {
            target.add(s)
        }
    }
}
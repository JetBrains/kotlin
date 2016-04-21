// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'filterNotTo(){}'"
fun foo(list: List<String>, target: MutableList<String>) {
    <caret>for (s in list) {
        if (s.length == 0) continue
        target.add(s)
    }
}
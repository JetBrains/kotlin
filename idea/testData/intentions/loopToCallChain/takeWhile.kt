// WITH_RUNTIME
// INTENTION_TEXT: "Replace with '+= takeWhile{}'"
fun foo(list: List<String>, target: MutableCollection<String>) {
    <caret>for (s in list) {
        if (s.isEmpty()) break
        target.add(s)
    }
}
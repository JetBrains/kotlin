// WITH_RUNTIME
// INTENTION_TEXT: "Replace with '+= mapIndexed{}'"
fun foo(list: List<String>, target: MutableList<Int>) {
    <caret>for ((index, s) in list.withIndex()) {
        target.add(s.hashCode() * index)
    }
}
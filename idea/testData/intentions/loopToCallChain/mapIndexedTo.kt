// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'mapIndexedTo(){}'"
fun foo(list: List<String>, target: MutableList<Int>) {
    <caret>for ((index, s) in list.withIndex()) {
        target.add(s.hashCode() * index)
    }
}
// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'filterIndexedTo(){}'"
fun foo(list: List<String>, target: MutableList<String>) {
    <caret>for ((index, s) in list.withIndex()) {
        if (s.length > index)
            target.add(s)
    }
}
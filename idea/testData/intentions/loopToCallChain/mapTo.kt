// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'filter{}.mapTo(){}'"
fun foo(list: List<String>, target: MutableList<Int>) {
    <caret>for (s in list) {
        if (s.length > 0)
            target.add(s.hashCode())
    }
}
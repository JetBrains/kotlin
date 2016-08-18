// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'mapTo(){}'"
// IS_APPLICABLE_2: false
fun foo(list: List<String>, target: MutableList<Int>) {
    <caret>for (s in list) {
        val l = s.length
        target.add(l)
    }
}
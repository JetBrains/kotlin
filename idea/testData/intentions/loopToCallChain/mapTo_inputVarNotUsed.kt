// WITH_RUNTIME
// IS_APPLICABLE: false
// IS_APPLICABLE_2: false
fun foo(list: List<String>, target: MutableList<Int>) {
    <caret>for (s in list) {
        if (s.length > 0)
            target.add(0)
    }
}
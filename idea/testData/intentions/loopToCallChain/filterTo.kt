// WITH_RUNTIME
fun foo(list: List<String>, target: MutableList<String>) {
    <caret>for (s in list) {
        if (s.length > 0)
            target.add(s)
    }
}
// WITH_RUNTIME
fun foo(list: List<String>, target: MutableList<String>) {
    <caret>for (s in list) {
        for (line in s.lines()) {
            target.add(line)
        }
    }
}
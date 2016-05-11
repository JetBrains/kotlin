// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'filter{}.forEach{}'"
fun foo(list: List<String>) {
    <caret>for (s in list) {
        if (s.isNotBlank()) {
            println(s)
        }
    }
}
// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'filterNotTo(){}'"
// IS_APPLICABLE_2: false
fun foo(list: List<String>, target: MutableList<String>) {
    <caret>for (s in list) {
        if (bar(s)) continue
        target.add(s)
    }
}

fun bar(string: String): Boolean = TODO()
// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'filterTo(){}'"
// IS_APPLICABLE_2: false
fun foo(list: List<String>) {
    val target = createCollection()
    <caret>for (s in list) {
        if (s.length > 0)
            target.add(s)
    }
}

fun createCollection() = java.util.ArrayList<String>()